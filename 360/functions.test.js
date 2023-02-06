'use strict';

const functions = require('./functions.js');

describe('functions', () => {
    describe('getAuth', () => {
        it('returns auth data', () => {
            const req = {
                core: {
                    auth: {
                        get: jest.fn().mockReturnValue(1)
                    }
                }
            };

            expect(functions.getAuth(req)).toBe(1);
        });
    });

    describe('getLang', () => {
        it('returns current language', () => {
            const req = {
                config: {
                    auth: { locale: 'foo' },
                    langs: [ 'foo', 'bar' ]
                }
            };

            expect(functions.getLang(req)).toBe('foo');
        });

        it('returns default current language', () => {
            const req = {
                config: {
                    auth: { locale: 'foo' },
                    defaultLang: 'bar',
                    langs: []
                }
            };

            expect(functions.getLang(req)).toBe('bar');
        });
    });
});
