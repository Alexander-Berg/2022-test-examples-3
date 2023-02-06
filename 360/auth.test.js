'use strict';

const Auth = require('./auth.js').default;
const {
    EXTERNAL_ERROR,
    AUTH_ERROR
} = require('../../lib/helpers/errors/index.js');

let auth;
let core;

beforeEach(() => {
    core = {};
    auth = new Auth(core);
});

describe('#set', () => {
    it('error case', () => {
        auth.set(new EXTERNAL_ERROR({ some: 'data' }));

        expect(auth.get()).toEqual({ error: { some: 'data' } });
        expect(auth.get()).toBeInstanceOf(EXTERNAL_ERROR);
    });
});

describe('#get', () => {
    it('должен вернуть данные', () => {
        auth.set({ uid: 'test' });

        expect(auth.get()).toEqual({ uid: 'test' });
    });

    it('должен вернуть ошибку AUTH_ERROR когда нет _data', () => {
        const error = auth.get();
        expect(error).toBeInstanceOf(AUTH_ERROR);
        expect(error.error).toEqual({ code: 'AUTH_UNKNOWN' });
    });

    it('должен вернуть ошибку AUTH_ERROR с кодом AUTH_UNKNOWN', () => {
        auth.set({ error: {} });

        const error = auth.get();
        expect(error).toBeInstanceOf(AUTH_ERROR);
        expect(error.error).toEqual({ code: 'AUTH_UNKNOWN' });
    });

    it('должен вернуть ошибку AUTH_ERROR c правильным кодом', () => {
        auth.set({ error: new EXTERNAL_ERROR({ code: 2001 }) });

        const error = auth.get();
        expect(error).toBeInstanceOf(AUTH_ERROR);
        expect(error.error).toEqual({ code: 'AUTH_NO_AUTH' });
    });
});
