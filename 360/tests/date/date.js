const date = require('../../lib/date/date');
const expect = require('expect');

describe('date/date', () => {
    const runTest = (dateGiven, now, expectedResult) => {
        const testName = `${dateGiven} was a day before ${now} should return "${expectedResult}"`;
        it(testName, () => {
            expect(date.wasItYesterday(dateGiven, now)).toEqual(expectedResult);
        });
    };

    runTest(new Date(2016, 2, 1, 13, 20), undefined, false);
    runTest(new Date(2016, 2, 1, 13, 20), new Date(), false);
    runTest(new Date(2016, 2, 1, 13, 20), new Date(2016, 2, 2, 13, 20), true);
    runTest(new Date(1910, 2, 2), undefined, false);

    const yesterday = date.getDaysAgoDate(new Date(), 1);
    runTest(yesterday, undefined, true);

    [
        ['Mon Dec 12 2016 18:28:48 GMT+0300 (MSK)', 'Mon Dec 13 2016 18:28:48 GMT+0300 (MSK)', true],
        ['Mon Dec 12 2016 00:00:01 GMT+0300 (MSK)', 'Mon Dec 13 2016 23:59:59 GMT+0300 (MSK)', true],
        ['Sat Feb 28 2015 18:28:48 GMT+0300 (MSK)', 'Thu Mar 1 2015 8:28:48 GMT+0300 (MSK)', true],
        ['Wed Feb 29 2012 18:28:48 GMT+0300 (MSK)', 'Thu Mar 1 2012 8:28:48 GMT+0300 (MSK)', true],
        ['Wed Nov 30 2016 18:28:48 GMT+0300 (MSK)', 'Thu Dec 1 2016 18:28:48 GMT+0300 (MSK)', true],
        ['Sun May 31 2015 18:28:48 GMT+0300 (MSK)', 'Mon Jun 1 2015 18:28:48 GMT+0300 (MSK)', true],
        ['Mon Dec 11 2016 23:59:59 GMT+0300 (MSK)', 'Mon Dec 13 2016 00:00:01 GMT+0300 (MSK)', false],
        // в високосном году в феврале 29 дней, а не 28
        ['Tur Feb 28 2012 18:28:48 GMT+0300 (MSK)', 'Thu Mar 1 2012 8:28:48 GMT+0300 (MSK)', false]

    ].forEach(([date, now, expectedResult]) => {
        // @ts-ignore
        runTest(new Date(date), new Date(now), expectedResult);
    });
});
