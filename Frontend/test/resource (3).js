/* global describe, it, beforeEach */

var assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert;
var path = require('path'),
    util = require('nodules-libs').util,
    extend = require('extend');

describe('CoreResource', function() {
    var app = require('../app'),
        CoreResource = app.Resource,
        ResourceError = CoreResource.ResourceError,
        Resource = require('resource');

    ResourceError.setLogger(function() {});

    it('should be an inheritor of Resource', function() {
        assert.isTrue(Resource.isParentOf(CoreResource));
    });

    describe('constructor', function() {
        var TestResource,
            servant = {
                _backendHost: 'foo.bar',
                _backendPort: Math.random(),
                _agents: {
                    common: {
                        name: 'foobar',
                        maxSockets: Math.random()
                    }
                }
            };

        beforeEach(function() {
            TestResource = CoreResource
                .create()
                .setEnvConfig(servant);
        });

        it('should set host, port and agent from config', function() {
            var instance = new TestResource();

            assert.strictEqual(instance.cfg.host, servant._backendHost);
            assert.strictEqual(instance.cfg.port, servant._backendPort);
            assert.strictEqual(instance.cfg.agent, servant._agents.common);
        });

        it('shouldn’t set port if handle has host', function() {
            var host = 'test.com',
                instance = new TestResource({ host: host });

            assert.strictEqual(instance.cfg.host, host);
            assert.isUndefined(instance.cfg.port);
        });
    });

    describe('method', function() {
        var TestResource,
            coreResource,
            logId = 'test/resource';

        beforeEach(function() {
            TestResource = CoreResource.create();

            TestResource.cfg = {
                path: '/foo/bar',
                isMandatory: true
            };

            coreResource = new TestResource(extend({ logId : logId }, TestResource.cfg));
        });

        describe('.setPath()', function() {
            it('should save resource path in .path but should not change parent path', function() {
                var TestResource = CoreResource.create(),
                    resourceRoot = 'foobar';

                TestResource.setPath(resourceRoot);

                assert.equal(TestResource.path, resourceRoot);
                assert.notEqual(CoreResource.path, resourceRoot);
            });
        });

        describe('.requireResource', function() {
            it('should require resource from path', function() {
                var TestResource = CoreResource.create(),
                    requiredResource;

                TestResource.path = path.resolve(__dirname, 'mocks');

                requiredResource = TestResource.requireResource('require-resource');

                assert.equal(requiredResource, 'it is require-resource file');
            });
        });

        describe('#processResultData()', function() {
            var resourceError = {
                    code: 'UNEXPECTED_RESULT'
                },
                applicationError1 = {
                    code: 'SOME_APPLICATION_ERROR'
                },
                applicationError2 = {
                    code: 'ANOTHER_APPLICATION_ERROR'
                };

            it('should throw and log ResourceError', function() {
                var response = {
                        errors: [ applicationError1, resourceError ]
                    };

                assert.throwTerror(function() {
                    coreResource.processResultData(response);
                }, CoreResource.ResourceError, 'RESOURCE_ERROR');

                assert.logsTerror(CoreResource.ResourceError, resourceError.code, function() {
                    coreResource.processResultData(response);
                });
            });

            it('should save non-resource errors in response.errors (without nulls)', function() {
                var response = {
                        errors: [ applicationError1, null, applicationError2 ]
                    },
                    result = coreResource.processResultData(response);

                assert.lengthOf(result.errors, 2);
                assert.include(result.errors, applicationError1);
                assert.include(result.errors, applicationError2);
            });

            it('should extend non-resource with resourceId, url and backendError', function() {
                var error = { code: 'INTERNAL_CASSANDRA_ERROR' },
                    response = { errors: [ error ] },
                    result = coreResource.processResultData(response);

                assert.deepEqual(result.errors[0], util.extend({}, error, {
                    resource: logId,
                    url: '',
                    originalError: '{\n  "code": "INTERNAL_CASSANDRA_ERROR"\n}'
                }));
            });

            it('should throw an EMPTY_DATA error, if there are no errors and data in response', function() {
                assert.throwTerror(function() {
                    coreResource.processResultData({});
                }, CoreResource.ResourceError, 'EMPTY_RESPONSE');
            });

            it('should return null if there are errors in response but resource is not mandatory', function() {
                coreResource.cfg.isMandatory = false;

                assert.isNull(coreResource.processResultData({ errors: [ { code: 'error' } ] }));
            });

            it('should return null if there no data and errors in response but resource is not mandatory', function() {
                coreResource.cfg.isMandatory = false;

                assert.isNull(coreResource.processResultData({}));
            });
        });
    });
});
