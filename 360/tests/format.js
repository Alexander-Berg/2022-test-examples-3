const format = require('../lib/format');
const expect = require('expect');

describe('format', () => {
    describe('shortenName', () => {
        const runTest = (str, first, last, ellipsis, expectedString) => {
            // eslint-disable-next-line max-len
            const testName = `${str} with ${ellipsis} ${first} first and ${last} last letters left should return "${expectedString}"`;
            it(testName, () => {
                expect(format.shortenName(str, first, last, ellipsis)).toEqual(expectedString);
            });
        };

        runTest('commented on link', 2, 3, undefined, 'co...ink');
        runTest('commented on link', undefined, 3, undefined, 'comm...ink');
        runTest('commented on link', undefined, undefined, undefined, 'comm...nk');
        runTest('commented on link', 12, 14, undefined, 'commented on link');
        runTest('added 1 file to shared folder', 2, 3, '...', 'ad...der');
        runTest('added 1 file to shared folder', 6, 1, ',,,', 'added ,,,r');
        runTest('added 1 file to shared folder', 1, 5, '*', 'a*older');
    });
});
