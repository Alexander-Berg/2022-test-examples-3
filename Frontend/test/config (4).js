/* global describe, it, beforeEach */

/* eslint-disable import/no-extraneous-dependencies */

let assert = require('chai').assert;
let sinon = require('sinon');

describe('CoreConfig', function() {
    let app = require('../app');
    let Objex = require('objex');
    let CoreConfig = app.Config;

    it('should be an inheritor of Objex', function() {
        assert.isTrue(Objex.isParentOf(CoreConfig));
    });

    describe('method', function() {
        let TestConfig;

        beforeEach(function() {
            TestConfig = CoreConfig.create();
        });

        describe('.export()', function() {
            it('should return copy of Config.cfg', function() {
                assert.notStrictEqual(TestConfig.export(), TestConfig.cfg);
                assert.deepEqual(TestConfig.export(), TestConfig.cfg);
            });
        });

        describe('.set()', function() {
            it('should parse complex property name', function() {
                let prop = 'foo.bar';
                let value = { baz: 'quux' };

                TestConfig.set(prop, value);

                assert.strictEqual(TestConfig.cfg.foo.bar, value);
            });
        });

        describe('.setDebug()', function() {
            let _set;

            beforeEach(function() {
                _set = sinon.spy(TestConfig, 'set');
            });

            it('should call set(\'debug\', debug)', function() {
                TestConfig.setDebug(true);

                assert.isTrue(_set.calledWithExactly('debug', true));
            });

            it('should set debug equals to false if false passed', function() {
                TestConfig.setDebug(false);

                assert.isTrue(_set.calledWithExactly('debug', false));
            });
        });

        describe('.setEnv()', function() {
            let _set;

            beforeEach(function() {
                _set = sinon.spy(TestConfig, 'set');
            });

            it('should set the passed environment', function() {
                let env = 'some_env_stirng';

                TestConfig.setEnv(env);

                assert.isTrue(_set.calledWithExactly('env', env));
            });
        });

        describe('.setAvailableLangs()', function() {
            let langs = ['ru', 'ua', 'en', 'by'];

            it('should set lang list for tld list', function() {
                TestConfig.setAvailableLangs(['ru', 'ua'], langs);

                assert.deepEqual(TestConfig.cfg.languages.ru, { langs: langs });
                assert.deepEqual(TestConfig.cfg.languages.ua, { langs: langs });
            });

            it('should set lang list for single tld', function() {
                TestConfig.setAvailableLangs('com', langs);

                assert.deepEqual(TestConfig.cfg.languages.com, { langs: langs });
            });
        });

        describe('.setStaticHost()', function() {
            it('should set cfg.staticHost[type] equals to host', function() {
                let type = 'some_static_type';
                let host = '//yandex.st';

                TestConfig.setStaticHost(type, host);

                assert.strictEqual(TestConfig.cfg.staticHost[type], host);
            });
        });

        describe('.setCommonAgent()', function() {
            it('should set cfg.servant._agents.common', function() {
                let commonAgent = {
                    name: 'common-backend',
                    maxSockets: 64,
                };

                TestConfig.setCommonAgent(commonAgent);

                assert.strictEqual(TestConfig.cfg.servant._agents.common, commonAgent);
            });
        });

        describe('.setServantHandle()', function() {
            it('should set handle with default host and port if only handle name was passed', function() {
                let defaultHost = 'some.host.com';
                let defaultPort = 1234;
                let handleName = 'some-backend-handle';

                TestConfig.cfg.servant._backendHost = defaultHost;
                TestConfig.cfg.servant._backendPort = defaultPort;

                TestConfig.setServantHandle(handleName);

                assert.property(TestConfig.cfg.servant, handleName);
                assert.deepEqual(TestConfig.cfg.servant[handleName], {
                    host: defaultHost,
                    port: defaultPort,
                    agent: TestConfig.cfg.servant._agents.common,
                });
            });

            it('should set handle properly if object was passed', function() {
                let host = 'some.host.com';
                let port = 1234;
                let handleName = 'some-backend-handle';

                TestConfig.setServantHandle({
                    handle: handleName,
                    host: host,
                    port: port,
                });

                assert.property(TestConfig.cfg.servant, handleName);
                assert.deepEqual(TestConfig.cfg.servant[handleName], {
                    host: host,
                    port: port,
                    agent: TestConfig.cfg.servant._agents.common,
                });
            });

            it('should not set port if only host was passed', function() {
                let host = 'some.host.com';
                let handleName = 'some-backend-handle';

                TestConfig.cfg.servant._backendPort = 1234;

                TestConfig.setServantHandle({
                    handle: handleName,
                    host: host,
                });

                assert.deepEqual(TestConfig.cfg.servant[handleName], {
                    host: host,
                    agent: TestConfig.cfg.servant._agents.common,
                });
            });

            it('should set port and default host if only port was passed', function() {
                let defaultHost = 'some.host.com';
                let port = 1234;
                let handleName = 'some-backend-handle';

                TestConfig.cfg.servant._backendHost = defaultHost;

                TestConfig.setServantHandle({
                    handle: handleName,
                    port: port,
                });

                assert.deepEqual(TestConfig.cfg.servant[handleName], {
                    host: defaultHost,
                    port: port,
                    agent: TestConfig.cfg.servant._agents.common,
                });
            });

            it('should set default agent', function() {
                TestConfig.setServantHandle('some-handle');
                TestConfig.setServantHandle({ handle: 'some-handle2', backendHost: 'foo' });

                assert.strictEqual(TestConfig.cfg.servant['some-handle'].agent, TestConfig.cfg.servant._agents.common);
                assert.strictEqual(TestConfig.cfg.servant['some-handle2'].agent, TestConfig.cfg.servant._agents.common);
            });

            it('should pass to servant other params', function() {
                let httpsHandle = { handle: 'https-handle', port: 443, protocol: 'https:', ca: './MyInternalCa.pem' };

                TestConfig.setServantHandle(httpsHandle);

                assert.strictEqual(TestConfig.cfg.servant[httpsHandle.handle].protocol, httpsHandle.protocol);
                assert.strictEqual(TestConfig.cfg.servant[httpsHandle.handle].ca, httpsHandle.ca);
                assert.isUndefined(TestConfig.cfg.servant[httpsHandle.handle].handle);
            });
        });

        describe('servant-setter', function() {
            let backendHandles = ['handle1', 'handle2', { handle: 'non-standard-handle', port: 1234 }];
            let setServantHandle;

            beforeEach(function() {
                setServantHandle = sinon.spy(TestConfig, 'setServantHandle');
            });

            function checkSetServantHandleCalls(testedHandles) {
                testedHandles.forEach(function(handle, index) {
                    assert.strictEqual(setServantHandle.args[index][0], handle);
                });
            }

            describe('.setBackend()', function() {
                let host = 'some.host.com';
                let port = 4321;

                it('should save backend host and port in appropriate properties', function() {
                    TestConfig.setBackend(host, port);

                    assert.strictEqual(TestConfig.cfg.servant._backendHost, host);
                    assert.strictEqual(TestConfig.cfg.servant._backendPort, port);
                });
            });

            describe('.setServantHandles()', function() {
                beforeEach(function() {
                    TestConfig.cfg.servant._backendHost = 'some.host.com';
                    TestConfig.cfg.servant._backendPort = 4321;
                });

                it('should call setServantHandle for each handle in passed array', function() {
                    TestConfig.setServantHandles(backendHandles);

                    checkSetServantHandleCalls(backendHandles);
                });

                it('should call setServantHandle for each handle passed as argument', function() {
                    TestConfig.setServantHandles.apply(TestConfig, backendHandles);

                    checkSetServantHandleCalls(backendHandles);
                });
            });
        });
    });
});
