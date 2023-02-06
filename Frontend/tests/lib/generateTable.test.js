const assert = require('assert');
const fs = require('fs');
const catchError = require('catch-error-async');

const generateTable = require('lib/generateTable');

const data = [
    { col1: 'test11', col2: 'test12', col3: 'test13' },
    { col1: 'test21', col2: 'test22', col3: 'test23' },
    { col1: 'test31', col2: 'test32', col3: 'test33' },
];
const columns = { col1: 'Column 1', col2: 'Column 2', col3: 'Column 3' };

describe('generateTable library', () => {
    describe('generateTable', () => {
        it('should format matrix by csv', () => {
            const buffer = fs.readFileSync('tests/data/out.csv');

            assert.equal(generateTable({ format: 'csv', data, columns, bookName: 'Registrations' }), buffer.toString());
        });

        it('should format matrix by xlsx', () => {
            const buffer = fs.readFileSync('tests/data/out.xlsx');

            assert(
                Buffer.compare(generateTable({ format: 'xlsx', data, columns, bookName: 'Registrations' }), buffer),
                'Buffers not equals',
            );
        });

        it('should throw error if format is invalid"', async() => {
            const error = await catchError(generateTable, { format: 'inv@lid', data, columns });

            assert.equal(error.message, 'Unknown format');
        });
    });
});
