const assert = require('assert');
const fs = require('fs');
const catchError = require('catch-error-async');

const parseTable = require('lib/parseTable');

const testMatrix = [
    { 'Column 1': 'test11', 'Column 2': 'test12', 'Column 3': 'test13' },
    { 'Column 1': 'test21', 'Column 2': 'test22', 'Column 3': 'test23' },
    { 'Column 1': 'test31', 'Column 2': 'test32', 'Column 3': 'test33' },
];

describe('parseTable library', () => {
    describe('parseTable', () => {
        it('should parse matrix by csv', () => {
            const buffer = fs.readFileSync('tests/data/out.csv');

            assert.deepEqual(parseTable(buffer), testMatrix);
        });

        it('should parse matrix by xlsx', () => {
            const buffer = fs.readFileSync('tests/data/out.xlsx');

            assert.deepEqual(parseTable(buffer), testMatrix);
        });

        it('should throw error if file is invalid"', async() => {
            const error = await catchError(parseTable, 'inv@lid');

            assert.equal(error.message, 'Argument should be a Buffer');
        });
    });
});
