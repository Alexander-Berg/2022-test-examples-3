'use strict';

jest.mock('@ps-int/mail-lib');

const { BaseMethod } = require('@ps-int/mail-lib');
const GetConfig = require('./get-config.js');

describe('GetConfig', () => {
    it('returns request config', async () => {
        const getConfig = new GetConfig();
        const ctx = { core: { req: { config: 1 } } };

        expect(getConfig).toBeInstanceOf(BaseMethod);
        expect(await getConfig.call(ctx)).toBe(1);
    });
});
