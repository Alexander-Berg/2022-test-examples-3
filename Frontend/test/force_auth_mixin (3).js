/* global describe, it, beforeEach */

var assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    util = require('nodules-libs').util,
    Url = require('nodules-libs').Url;

describe('ForceAuthMixin', function() {
    var app = require('../app'),
        Controller = app.Controller,
        CoreControllerError = Controller.CoreControllerError,
        ForceAuthMixin = app.ForceAuthMixin,
        createMockUser = require('./mocks/user'),
        createMockParams = require('./mocks/controller_params');

    describe('Mixin', function() {
        it('should be mixed using `CoreController.mixin()` properly', function() {
            assert.canBeMixed(Controller.create(), ForceAuthMixin,
                'ForceAuthMixin', [ 'name', 'prototype' ], [ 'constructor' ]);
        });

        it('should add checkAuth function to before chain', function() {
            var TestController = Controller
                    .create()
                    .mixin(ForceAuthMixin),
                before = TestController.prototype._beforeChain;

            assert.strictEqual(before[before.length - 1], ForceAuthMixin.prototype.checkUserAuth);
        });
    });

    describe('method', function() {
        var TestController = Controller
                .create()
                .mixin(ForceAuthMixin),
            testController,
            PASSPORT_BASE = 'https://passport.yandex',
            CURRENT_URL = 'http://wmfront.yandex.ru/app-core';

        function createAuthUser(authType) {
            var user = createMockUser({
                auth: {
                    isAuth: true
                }
            });

            user.auth[authType] = true;

            return user;
        }

        function createLightUser() {
            return createAuthUser('isLiteUser');
        }

        function createLightAuthUser() {
            return createAuthUser('isLiteAuth');
        }

        beforeEach(function() {
            testController = new TestController(createMockParams({
                req: {
                    urlHelper: new Url({
                        url: CURRENT_URL,
                        routers: {
                            desktop: require('./mocks/susanin-for-make-url')
                        }
                    })
                }
            }));
        });

        describe('#getPassportHost()', function() {
            var tlds = [
                    'com',
                    'com.tr',
                    'ru',
                    'ua',
                    'by'
                ];

            function getConditionText(tld) {
                return util.format('should return %s.tld if current region is %s', PASSPORT_BASE, tld, tld);
            }

            function getPassportHost(zone) {
                return util.format('%s.%s', PASSPORT_BASE, zone);
            }

            tlds.forEach(function(tld) {
                var zoneTestController = new TestController(createMockParams({
                    req: {
                        urlHelper: new Url({
                            url: 'http://wmfront.yandex.' + tld,
                            routers: {
                                desktop: require('./mocks/susanin-for-make-url')
                            }
                        })
                    }
                }));

                it(getConditionText(tld), function() {
                    assert.strictEqual(zoneTestController.getPassportHost(), getPassportHost(tld));
                });
            });
        });

        describe('#throwAuthRedirectError()', function() {
            function getPassportURL(mode) {
                // https://passport.yandex.ru/passport?mode=auth&retpath=http%3A%2F%2Fwmfront.yandex.ru%2Fapp-core
                return util.format('%s.ru/passport?mode=%s&retpath=%s',
                    PASSPORT_BASE, mode, encodeURIComponent(CURRENT_URL));
            }

            function assertAuthRedirectMode(mode) {
                try {
                    testController.throwAuthRedirectError();
                }
                catch (error) {
                    assert.instanceOf(error, CoreControllerError);
                    assert.strictEqual(error.code, CoreControllerError.CODES.REDIRECT);
                    assert.strictEqual(error.data.location, getPassportURL(mode));
                }
            }

            it('should redirect light user to light2full authorization', function() {
                testController.user = createLightUser();

                assertAuthRedirectMode('light2full');
            });

            it('should redirect light-auth user to lightauth2full authorization', function() {
                testController.user = createLightAuthUser();

                assertAuthRedirectMode('lightauth2full');
            });

            it('should redirect other kind of users to auth authorization', function() {
                testController.user = createMockUser();

                assertAuthRedirectMode('auth');
            });

        });

        describe('#checkUserAuth()', function() {
            var ALLOW_LIGHT = 'allow-lite';
            
            function assertThrowRedirect() {
                assert.throwTerror(function() {
                    testController.checkUserAuth();
                }, CoreControllerError, 'REDIRECT');
            }

            function assertDoesNotThrowRedirect() {
                assert.doesNotThrow(function() {
                    testController.checkUserAuth();
                }, CoreControllerError);
            }

            it('should redirect unauthorized user to passport if force-auth === true or lite-auth', function() {
                testController.user = createMockUser();

                testController['force-auth'] = true;
                assertThrowRedirect();

                testController['force-auth'] = ALLOW_LIGHT;
                assertThrowRedirect();
            });

            it('should redirect light-auth user to passport only if force-auth === true', function() {
                testController.user = createLightAuthUser();

                testController['force-auth'] = true;
                assertThrowRedirect();

                testController['force-auth'] = ALLOW_LIGHT;
                assertDoesNotThrowRedirect();
            });

            it('should redirect light user to passport only if force-auth === true', function() {
                testController.user = createLightUser();

                testController['force-auth'] = true;
                assertThrowRedirect();

                testController['force-auth'] = ALLOW_LIGHT;
                assertDoesNotThrowRedirect();
            });
        });
    });

});
