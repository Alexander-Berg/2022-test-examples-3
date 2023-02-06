/* global describe, it, beforeEach */

var assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    sinon = require('sinon'),
    path = require('path');

describe('CoreTemplatedGate', function() {
    var app = require('../app'),
        CoreTemplatedGate = app.TemplatedGate,
        CoreGate = app.Gate,
        Resource = app.Resource,
        privPath = path.resolve(__dirname, 'mocks/priv');

    it('should be an inheritor of CoreGate with mixed TemplateMixin', function() {
        assert.isTrue(CoreGate.isParentOf(CoreTemplatedGate));

        assert.isMixed(CoreTemplatedGate, app.TemplateMixin, 'CoreTemplateMixin',
            [ 'name', 'prototype', 'super_', 'create', '__super', '__objexOnMixing' ],
            [ 'constructor' ]);
    });

    describe('method', function() {
        describe('.setEnv()', function() {
            var TestTemplatedGate;

            beforeEach(function() {
                TestTemplatedGate = CoreTemplatedGate.create('TestTemplatedGate');
            });

            it('should set __templateConfig property if opts.config passed', function() {
                var config = require('./fakes/config'),
                    setConfig = sinon.spy(TestTemplatedGate, 'setConfig');

                TestTemplatedGate.setEnv({ config: config });

                assert.isTrue(setConfig.calledWith(config));

                setConfig.restore();
            });

            it('should set __resource property if opts.resource was passed', function() {
                var ResourceExecutor = Resource.create(),
                    setResourceExecutor = sinon.spy(TestTemplatedGate, 'setResourceExecutor');

                TestTemplatedGate.setEnv({ resource: ResourceExecutor });

                assert.isTrue(setResourceExecutor.calledWith(ResourceExecutor));

                setResourceExecutor.restore();
            });

            it('should set _templatePriv property if opts.priv and opts.config were passed', function() {
                var setPriv = sinon.spy(TestTemplatedGate, 'setPriv');

                TestTemplatedGate.setEnv({
                    config: { debug: true },
                    priv: privPath
                });

                assert.isTrue(setPriv.calledWith(privPath, true));

                setPriv.restore();
            });
        });
    });
});
