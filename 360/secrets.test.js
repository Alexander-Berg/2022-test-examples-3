'use strict';

jest.mock('/var/wwwroot/mail/server/secrets.json', () => ({ foo: 'bar' }), { virtual: true });

describe('secrets', () => {
    it('for coverage', () => {
        const secrets = require('./secrets.js');
        expect(secrets).toEqual({ foo: 'bar' });
    });
});
