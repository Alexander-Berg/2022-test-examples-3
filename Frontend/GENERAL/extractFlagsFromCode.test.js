'use strict';

const { assert } = require('chai');

const extractFlagsFromCode = require('./extractFlagsFromCode');

describe('tools / helpers / extractFlagsFromCode', () => {
    describe('plain code', () => {
        it('should find all flags', () => {
            const input = `
if (context.expFlags.foo && context.reportData.expFlags['foo-1']) return 1;
if (data.reqdata.flags.bar && data.reqdata.flags['bar-2']) return 2;
            `;

            const actual = extractFlagsFromCode(input);
            const expected = ['foo', 'foo-1', 'bar', 'bar-2'];

            assert.deepEqual(actual, expected);
        });

        it('should find no flags', () => {
            const input = `
console.log("no flags here, sorry");
            `;

            const actual = extractFlagsFromCode(input);
            const expected = [];

            assert.deepEqual(actual, expected);
        });

        it('should ignore init_meta', () => {
            const input = `
if (context.expFlags.init_meta.enable_whatever) return 1;
            `;

            const actual = extractFlagsFromCode(input);
            assert.isEmpty(actual);
        });
    });

    describe('code diff', () => {
        it('should find only added flags', () => {
            const input = `
- if (data.expFlags.foo) return 1;
+ if (context.expFlags.bar) return 2;
            `;

            const actual = extractFlagsFromCode(input, '+ ');
            const expected = ['bar'];

            assert.deepEqual(actual, expected);
        });
    });
});
