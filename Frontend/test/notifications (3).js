/* global describe, it, beforeEach */

var assert = require('chai').assert;

describe('Notifications', function() {
    var Notifications = require('../app/lib/notifications'),
        NotificationsError = Notifications.NotificationsError,
        createMockParams = require('./mocks/controller_params'),
        notifications;

    it('should create new instance properly', function() {
        var notificationsCookie = { someparam: "someval" },
            reqRes = createMockParams({
                req: {
                    cookies: {
                        notifications: JSON.stringify(notificationsCookie)
                    }
                }
            });

        notifications = new Notifications(reqRes.req, reqRes.res);

        assert.strictEqual(notifications._req, reqRes.req);
        assert.strictEqual(notifications._res, reqRes.res);
        assert.deepEqual(notifications._data, notificationsCookie);
        assert.deepEqual(notifications._nextData, {});
        assert.deepEqual(notifications._cache, {});
    });

    describe('method', function() {
        var reqRes = createMockParams({
                req: {
                    cookies: {}
                }
            }),
            FIRST_TANKER_KEY = 'FIRST_TANKER_KEY',
            SECOND_TANKER_KEY = 'SECOND_TANKER_KEY',
            FIRST_NOTIFICATION = {
                type: 'error',
                key: FIRST_TANKER_KEY,
                params: {},
                escape: true,
                unique: false
            },
            SECOND_NOTIFICATION = {
                type: 'success',
                key: SECOND_TANKER_KEY,
                params: { counter: 42 },
                escape: true,
                unique: true
            },
            notifications;

        beforeEach(function() {
            notifications = new Notifications(reqRes.req, reqRes.res);
        });

        describe('#addKey()', function() {
            it('should add notifcation with defaults options if opts was not passed', function() {
                notifications.addKey(FIRST_TANKER_KEY);
                assert.deepEqual(notifications._data, { local: [ FIRST_NOTIFICATION ] });
            });

            it('should add notifcation with passed options', function() {
                notifications.addKey(SECOND_TANKER_KEY, {
                    type: SECOND_NOTIFICATION.type,
                    params: SECOND_NOTIFICATION.params,
                    unique: SECOND_NOTIFICATION.unique,
                    place: 'submit'
                });

                assert.deepEqual(notifications._data, { submit: [ SECOND_NOTIFICATION ] });
            });

            it('shouldn’t add notification if unique notification with the same key in this place exists', function() {
                notifications.addKey(SECOND_TANKER_KEY, {
                    place: 'submit',
                    type: SECOND_NOTIFICATION.type,
                    unique: true
                });

                notifications.addKey(SECOND_TANKER_KEY, {
                    place: 'submit',
                    type: 'success'
                });

                assert.lengthOf(notifications._data.submit, 1);
            });

            it('should save notification for the next page if forNext === true', function() {
                notifications.addKey(FIRST_TANKER_KEY, {
                    forNext: true
                });

                assert.deepEqual(notifications._nextData, {
                    local: [ FIRST_NOTIFICATION ]
                });
            });

            it('should set escape=false if error.escape is false', function() {
                notifications.addKey(FIRST_TANKER_KEY, {
                    escape: false
                });

                assert.deepEqual(notifications._data, {
                    local: [
                        {
                            type: FIRST_NOTIFICATION.type,
                            key: FIRST_NOTIFICATION.key,
                            params: FIRST_NOTIFICATION.params,
                            escape: false,
                            unique: FIRST_NOTIFICATION.unique
                        }
                    ]
                });
            });
        });

        describe('#addKeys()', function() {
            it('should add all notifications with passed options', function() {
                notifications.addKeys([ FIRST_TANKER_KEY, SECOND_TANKER_KEY ], {
                    place: 'form',
                    type: 'error'
                });

                assert.deepEqual(notifications._data, {
                    form: [
                        {
                            type: 'error',
                            key: FIRST_TANKER_KEY,
                            params: {},
                            escape: true,
                            unique: false
                        },
                        {
                            type: 'error',
                            key: SECOND_TANKER_KEY,
                            params: {},
                            escape: true,
                            unique: false
                        }
                    ]
                });
            });
        });

        describe('#get()', function() {
            it('should return a copy of array containing notifications in passed place', function() {
                notifications._data = {
                    local: [ FIRST_NOTIFICATION, SECOND_NOTIFICATION ]
                };

                assert.notStrictEqual(notifications.get('local'), notifications._data.local);
                assert.sameMembers(notifications.get('local'), notifications._data.local);
            });

            it('should return an empty array if passed place doesn’t exist', function() {
                var notificationsCookie = notifications.get('local');

                assert.isArray(notificationsCookie);
                assert.lengthOf(notificationsCookie, 0);
            });
        });

        describe('#save()', function() {
            function parseCookie() {
                var cookies = {};

                reqRes.res.headers['set-cookie'].split(';').forEach(function(cookie) {
                    var res = cookie.split('=');

                    cookies[res[0]] = typeof res[1] === 'undefined' ? true : res[1];
                });

                cookies.expires = Date.parse(cookies.expires);

                return cookies;
            }

            it('should set cookie', function() {
                var cookies,
                    approximateCookieTime;

                notifications._nextData = {
                    local: [ FIRST_NOTIFICATION, SECOND_NOTIFICATION ]
                };

                approximateCookieTime = new Date().valueOf() + 24 * 3600 * 1000;
                notifications.save();
                cookies = parseCookie();

                assert.closeTo(cookies.expires, approximateCookieTime, 1000);
                assert.strictEqual(cookies.path, '/');
                assert.property(cookies, 'HttpOnly');
                assert.strictEqual(decodeURIComponent(cookies.notifications),
                    JSON.stringify(notifications._nextData));
            });

            it('should clear cookie if _nextData is emtpy but _data is not', function() {
                var cookies,
                    startOfTime;

                notifications._data = {
                    local: [ FIRST_NOTIFICATION ]
                };

                startOfTime = new Date(0).getTime();
                notifications.save();
                cookies = parseCookie();

                assert.strictEqual(cookies.expires, startOfTime);
            });

            it('should clear cookie if cookie is invalid', function() {
                var cookies,
                    approximateCookieTime;

                notifications._dataInvalid = true;

                approximateCookieTime = new Date(0).getTime();
                notifications.save();
                cookies = parseCookie();

                assert.strictEqual(cookies.expires, approximateCookieTime);
            });
        });

        describe('#parseCookie()', function() {
            it('should set _data if cookie is valid', function() {
                var notificationsCookie = {
                    local: [ FIRST_NOTIFICATION ],
                    submit: [ SECOND_NOTIFICATION ]
                };

                delete notifications._data;
                notifications._req.cookies.notifications = JSON.stringify(notificationsCookie);
                notifications.parseCookie();

                assert.deepEqual(notifications._data, notificationsCookie);
            });

            it('should set _dataInvalid if cookie is invalid and log an error', function() {
                var logger = NotificationsError.prototype.logger,
                    isErrorLogged = false;

                NotificationsError.setLogger(function() {
                    isErrorLogged = true;
                });

                delete notifications._data;
                notifications._req.cookies.notifications = '{';
                notifications.parseCookie();

                assert.isTrue(notifications._dataInvalid);
                assert.isTrue(isErrorLogged);

                NotificationsError.setLogger(logger);
            });
        });

        describe('#getData()', function() {
            it('should return copy of its’s own _data property', function() {
                var gottenData = notifications.getData();

                assert.notStrictEqual(gottenData, notifications._data);
                assert.deepEqual(gottenData, notifications._data);
            });
        });
    });
});
