'use strict';

const removeSovetnikInfo = require('./../../../../middleware/sovetnik-disabled/remove-sovetnik-info');

describe('remove sovetnik info from cookie', () => {
    test('should correctly handle empty parameters', () => {
        removeSovetnikInfo();
    });

    test('should correctly handle request without \'svt\'', () => {
        let key;
        let value;
        let domain;

        const req = {
            cookies: {
                ys: 'dummy1#dummy2'
            }
        };

        const res = {
            cookie: (k, v, d) => {
                [key, value, domain] = [k, v, d];
            }
        };

        removeSovetnikInfo(req, res);

        expect(key).toBe('ys');
        expect(value).toBe('dummy1#dummy2');
        expect(domain).toHaveProperty('domain', '.yandex.ru');
    });

    test('should correctly handle request with \'svt\'', () => {
        let key;
        let value;
        let domain;

        const req = {
            cookies: {
                ys: 'dummy1#svt.1#dummy2'
            }
        };

        const res = {
            cookie: (k, v, d) => {
                [key, value, domain] = [k, v, d];
            }
        };

        removeSovetnikInfo(req, res);

        expect(key).toBe('ys');
        expect(value).toBe('dummy1#dummy2');
        expect(domain).toHaveProperty('domain', '.yandex.ru');
    });
});
