'use strict';

const { encrypt } = require('./../../../utils/crypto');
const Cookie = require('./../../functional/lib/cookie');

describe('Cookie', () => {
    describe('constructor', () => {
        test('should return instance of Cookie class', () => {
            const cookie = new Cookie('dummy', 'dummy');

            expect(cookie).toBeInstanceOf(Cookie);
        });
    });

    describe('getters', () => {
        describe('key', () => {
            test('should return correct key', () => {
                const expected = 'key';
                const cookie = new Cookie(expected);
                const actual = cookie.key;

                expect(actual).toBe(expected);
            });
        });

        describe('value', () => {
            test('should return undefined', () => {
                const cookie = new Cookie();
                const actual = cookie.value;

                expect(actual).toBeUndefined();
            });

            test('should return string value', () => {
                const expected = 'dummy';
                const cookie = new Cookie('key', expected);
                const actual = cookie.value;

                expect(actual).toBe(expected);
            });

            test('should return JSON value', () => {
                const expected = {
                    applicationName: 'Application name',
                    affId: 1234,
                    clid: 1234567,
                    offerEnabled: false,
                    vid: null,
                    optOutAccepted: null,
                    offerAccepted: null,
                    pricebarShown: true
                };

                const cookie = new Cookie('key', expected);
                const actual = cookie.value;

                expect(actual).toBe(expected);
            });

            test('should return decrypted string value', () => {
                const value = 'dummy';
                const encryptedValue = encrypt(value);
                const cookie = new Cookie('key', encryptedValue);
                const decryptedValue = cookie.value;

                expect(decryptedValue).toBe(value);
            });

            test('should return decrypted JSON value', () => {
                const value = {
                    applicationName: 'Application name',
                    affId: 1234,
                    clid: 1234567,
                    offerEnabled: false,
                    vid: null,
                    optOutAccepted: null,
                    offerAccepted: null,
                    pricebarShown: true
                };

                const encryptedValue = encrypt(JSON.stringify(value));
                const cookie = new Cookie('key', encryptedValue);
                const decryptedValue = cookie.value;

                expect(decryptedValue).toEqual(value);
            });
        });

        describe('max-age', () => {
            test('should return session cookie', () => {
                const expected = 0;
                const cookie = new Cookie('key', 'value');
                const actual = cookie.expires;

                expect(actual).toBe(expected);
            });

            test('should return correct max-age (number)', () => {
                const expected = new Date(Date.now() + 10000);
                const cookie = new Cookie('key', 'value', { maxAge: 10000 });
                const actual = cookie.expires;

                expect(actual.getTime()).toBeGreaterThanOrEqual(expected.getTime());
            });

            test('should return correct max-age (string)', () => {
                const expected = new Date(Date.now() + 10000);
                const cookie = new Cookie('key', 'value', { maxAge: '10000' });
                const actual = cookie.expires;

                expect(actual.getTime()).toBeGreaterThanOrEqual(expected.getTime());
            });
        });

        describe('expires', () => {
            test('should return session cookie', () => {
                const expected = 0;
                const cookie = new Cookie('key', 'value');
                const actual = cookie.expires;

                expect(actual).toBe(expected);
            });

            test('should return correct expires value', () => {
                const expected = new Date(Date.now());
                const cookie = new Cookie('key', 'value', { expires: expected });
                const actual = cookie.expires;

                expect(actual).toEqual(expected);
            });
        });

        describe('max-age vs expires', () => {
            test('max-age directive should takes priority over expires', () => {
                const expected = Date.now();
                const maxAge = 10000;
                const expires = new Date(expected - 10000);
                const cookie = new Cookie('key', 'value', { maxAge: maxAge, expires: expires });
                const actual = cookie.expires;

                expect(actual.getTime()).toBeGreaterThanOrEqual(new Date(expected).getTime());
            });
        });

        describe('path', () => {
            test('should return default path', () => {
                const expected = '/';
                const cookie = new Cookie('key', 'value');
                const actual = cookie.path;

                expect(actual).toBe(expected);
            });

            test('should return correct path', () => {
                const expected = 'dummy';
                const cookie = new Cookie('key', 'value', { path: expected });
                const actual = cookie.path;

                expect(actual).toBe(expected);
            });
        });

        describe('domain', () => {
            test('should return default domain', () => {
                const expected = 'sovetnik.market.yandex.ru';
                const cookie = new Cookie('key', 'value');
                const actual = cookie.domain;

                expect(actual).toBe(expected);
            });

            test('should return correct domain', () => {
                const expected = 'dummy';
                const cookie = new Cookie('key', 'value', { domain: expected });
                const actual = cookie.domain;

                expect(actual).toBe(expected);
            });
        });

        describe('secure', () => {
            test('should return default secure', () => {
                const expected = false;
                const cookie = new Cookie('key', 'value');
                const actual = cookie.secure;

                expect(actual).toBe(expected);
            });

            test('should return correct secure', () => {
                const expected = true;
                const cookie = new Cookie('key', 'value', { secure: expected });
                const actual = cookie.secure;

                expect(actual).toBe(expected);
            });
        });

        describe('httpOnly', () => {
            test('should return default httpOnly', () => {
                const expected = true;
                const cookie = new Cookie('key', 'value');
                const actual = cookie.httpOnly;

                expect(actual).toBe(expected);
            });

            test('should return correct httpOnly', () => {
                const expected = false;
                const cookie = new Cookie('key', 'value', { httpOnly: expected });
                const actual = cookie.httpOnly;

                expect(actual).toBe(expected);
            });
        });

        describe('isSession', () => {
            test('should correctly identify the session cookie (true)', () => {
                const expected = true;
                const cookie = new Cookie('key', 'value');
                const actual = cookie.isSession;

                expect(actual).toBe(expected);
            });

            test('should correctly identify the session cookie (true, second)', () => {
                const expected = true;
                const cookie = new Cookie('key', 'value', { expires: 0 });
                const actual = cookie.isSession;

                expect(actual).toBe(expected);
            });

            test('should correctly identify the session cookie (false)', () => {
                const expected = false;
                const cookie = new Cookie('key', 'value', { expires: new Date(Date.now()) });
                const actual = cookie.isSession;

                expect(actual).toBe(expected);
            });

            test('should correctly identify the session cookie (false, second)', () => {
                const expected = false;
                const cookie = new Cookie('key', 'value', { maxAge: 10 });
                const actual = cookie.isSession;

                expect(actual).toBe(expected);
            });
        });
    });
});
