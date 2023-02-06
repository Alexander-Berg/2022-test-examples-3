/* global describe, it */

const assert = require('assert');
const format = require('util').format;

const murmurHash3 = require('../');
const testData = require('./data');

describe('murmurHash3', () => {
    function makeSuite(name, cases) {
        describe(name, () => {
            cases.forEach(item => {
                let res = item[0];
                let str = item[1];
                let seed = item[2];

                it(format('should return %s if str = %s and seed = %s', res, str, seed), () => {
                    assert.equal(res, murmurHash3(str, seed));
                });
            });
        });
    }

    makeSuite('without remainder', testData.withoutRemainder);
    makeSuite('with remainder', testData.withRemainder);
});
