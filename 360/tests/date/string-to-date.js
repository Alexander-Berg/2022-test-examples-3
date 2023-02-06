const stringToDate = require('../../lib/date/string-to-date');

const expect = require('expect');

describe('date/string-to-date', () => {
    const runTest = (dateStr, format, expectedDate) => {
        const formatStr = format ? `and "${format}" format ` : '';
        const testName = `${dateStr} ${formatStr}should return "${expectedDate}"`;
        it(testName, () => {
            expect(stringToDate(dateStr, format)).toEqual(expectedDate);
        });
    };

    runTest('01.01.1970 00:00', undefined, new Date(1970, 0, 1));
    runTest('01.03.2017 13:20', undefined, new Date(2017, 2, 1, 13, 20));
    runTest('20170301', 'YYYYMMDD', new Date(2017, 2, 1));

    runTest('04$09;;2000 .. 030405', 'DD$MM;;YYYY .. HHmmss', new Date(2000, 8, 4, 3, 4, 5));
    runTest('6543210 упрт :)', 'smHMDYY', new Date(1910, 2, 2, 4, 5, 6));
});
