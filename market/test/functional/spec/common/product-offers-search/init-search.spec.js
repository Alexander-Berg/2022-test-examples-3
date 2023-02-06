'use strict';

const Ajv = require('ajv');

const INIT_SEARCH_MIDDLEWARE = require('./../../../../../middleware/product-offers-search/init-search');
const SEARCH_DATA_SCHEMA = require('./../../../data/search-data');
const Client = require('./../../../lib/client');

const ajv = new Ajv();

describe('Product offers search, init search middleware', () => {
    test('request should contain correct \'searchData\'', async (done) => {
        const client = new Client();

        await client.request([
            INIT_SEARCH_MIDDLEWARE,
            (req, res) => {
                try {
                    const searchData = req && req.searchData;
                    let isValidate = ajv.validate(SEARCH_DATA_SCHEMA.COMMON, searchData);

                    expect(isValidate).toBeTruthy();

                    isValidate = ajv.validate(SEARCH_DATA_SCHEMA.PRODUCT_INIT_SEARCH, searchData);

                    expect(isValidate).toBeTruthy();
                } catch (err) {
                    return done(err);
                }

                res.json();
                done();
            }
        ]);
    });
});
