'use strict';

jest.mock('@ps-int/mail-lib');

const { BaseMethod } = require('@ps-int/mail-lib');
const GetSecretKey = require('./get-secret-key.js');

describe('GetSecretKey', () => {
    it('returns request secret key', async () => {
        jest.spyOn(Date, 'now').mockImplementation(() => 15e11);

        const getSecretKey = new GetSecretKey();
        expect(getSecretKey).toBeInstanceOf(BaseMethod);

        const ctx = { headers: { 'x-uid': '12345' } };
        const { secretKey } = await getSecretKey.call(ctx);
        expect(secretKey).toEqual('u8c6c11e5ce1beaa2889938a8f52e610c');
    });
});
