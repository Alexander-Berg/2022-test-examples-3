const secondsToTime = require('../../lib/date/seconds-to-time');
const expect = require('expect');

describe('date/seconds-to-time', () => {
    const runTest = (sec, expectedResult, separator) => {
        const separatorStr = separator ? `with "${separator}" separator ` : '';
        const testName = `${sec} seconds ${separatorStr}should return "${expectedResult}"`;
        it(testName, () => {
            expect(secondsToTime(sec, separator)).toEqual(expectedResult);
        });
    };

    runTest(0, '00:00');
    runTest(1, '00:01');
    runTest(23, '00:23');
    runTest(59, '00:59');
    runTest(60, '01:00');
    runTest(138, '02:18');
    runTest(1956, '32:36');
    runTest(3599, '59:59');
    runTest(3600, '1:00:00');
    runTest(7541, '2:05:41');
    runTest(216000, '60:00:00');
    runTest(359999, '99:59:59');
    runTest(360000, '100:00:00');

    runTest(Number.MAX_SAFE_INTEGER, '2501999792983:36:31');

    runTest(-1, '-00:01');
    runTest(-59, '-00:59');
    runTest(-60, '-01:00');
    runTest(-7541, '-2:05:41');

    runTest(7541, '2~05~41', '~');
    runTest(-359999, '-99**59**59', '**');
});
