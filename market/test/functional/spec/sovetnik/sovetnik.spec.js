'use strict';

const Ajv = require('ajv');

const Client = require('./../../lib/client');
const common = require('./../../data/common');
const SOVETNIK_ROUTE = require('../../lib/routes').SOVETNIK;

const ajv = new Ajv();

describe('Save sovetnik info middleware', () => {
    it('should return { ok: true }', async () => {
        const client = new Client();
        const result = await client.request(SOVETNIK_ROUTE);
        const isOkTrue = ajv.validate(common.OK_TRUE, result.response);

        expect(isOkTrue).toBeTruthy();
    });
});
