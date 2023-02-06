const isBrowserSupported = require('../lib/is-browser-supported');

const expect = require('expect');

const UNSUPPORTED = [
    { isMobile: false, BrowserName: 'MSIE', BrowserVersion: '<11' },
    { isMobile: false, BrowserName: 'Firefox', BrowserVersion: '<17.5' },
    { isMobile: false, BrowserName: 'Opera', BrowserVersion: '<30.1.2' },
    { isMobile: false, BrowserName: 'Safari', BrowserVersion: '<7' },
    { BrowserName: 'Safari', OSName: 'Windows XP' }
];

const runTest = function(uaObject, expectedResult, namePostfix) {
    uaObject.isMobile = (uaObject.isMobile === true);
    const testName = uaObject.BrowserName + ' ' + uaObject.BrowserVersion +
        (namePostfix || '') + (uaObject.isMobile ? ' (mobile)' : '');
    it(testName, () => {
        expect(isBrowserSupported(uaObject, UNSUPPORTED)).toEqual(expectedResult);
    });
};

describe('is-browser-supported', () => {
    describe('IE', () => {
        runTest({ BrowserName: 'MSIE', BrowserVersion: '5.0' }, false);
        runTest({ BrowserName: 'MSIE', BrowserVersion: '6.1.6.7' }, false);
        runTest({ BrowserName: 'MSIE', BrowserVersion: '7.456' }, false);
        runTest({ BrowserName: 'MSIE', BrowserVersion: '8.444' }, false);
        runTest({ BrowserName: 'MSIE', BrowserVersion: '9' }, false);
        runTest({ BrowserName: 'MSIE', BrowserVersion: '10.999' }, false);
        runTest({ BrowserName: 'MSIE', BrowserVersion: '10' }, false);
        runTest({ BrowserName: 'MSIE', BrowserVersion: '11.0' }, true);
        runTest({ BrowserName: 'MSIE', BrowserVersion: '11' }, true);
    });
    describe('Firefox', () => {
        runTest({ BrowserName: 'Firefox', BrowserVersion: '1.0.0' }, false);
        runTest({ BrowserName: 'Firefox', BrowserVersion: '16' }, false);
        runTest({ BrowserName: 'Firefox', BrowserVersion: '16.999' }, false);
        runTest({ BrowserName: 'Firefox', BrowserVersion: '17' }, false);
        runTest({ BrowserName: 'Firefox', BrowserVersion: '17.0' }, false);
        runTest({ BrowserName: 'Firefox', BrowserVersion: '17.4' }, false);
        runTest({ BrowserName: 'Firefox', BrowserVersion: '17.4.999' }, false);
        runTest({ BrowserName: 'Firefox', BrowserVersion: '17.5' }, true);
        runTest({ BrowserName: 'Firefox', BrowserVersion: '17.5.0' }, true);
        runTest({ BrowserName: 'Firefox', BrowserVersion: '17.5.123' }, true);
        runTest({ BrowserName: 'Firefox', BrowserVersion: '58' }, true);
    });
    describe('Opera', () => {
        runTest({ BrowserName: 'Opera', BrowserVersion: '2' }, false);
        runTest({ BrowserName: 'Opera', BrowserVersion: '29' }, false);
        runTest({ BrowserName: 'Opera', BrowserVersion: '30' }, false);
        runTest({ BrowserName: 'Opera', BrowserVersion: '30.1' }, false);
        runTest({ BrowserName: 'Opera', BrowserVersion: '30.1.0' }, false);
        runTest({ BrowserName: 'Opera', BrowserVersion: '30.1.1' }, false);
        runTest({ BrowserName: 'Opera', BrowserVersion: '30.1.2' }, true);
        runTest({ BrowserName: 'Opera', BrowserVersion: '30.1.10' }, true);
        runTest({ BrowserName: 'Opera', BrowserVersion: '30.1.02' }, true);
        runTest({ BrowserName: 'Opera', BrowserVersion: '30.5' }, true);
        runTest({ BrowserName: 'Opera', BrowserVersion: '43' }, true);
    });
    describe('Safari', () => {
        runTest({ BrowserName: 'Safari', BrowserVersion: '2' }, false);
        runTest({ BrowserName: 'Safari', BrowserVersion: '6' }, false);
        runTest({ BrowserName: 'Safari', BrowserVersion: '7' }, true);
        runTest({ BrowserName: 'Safari', BrowserVersion: '12' }, true);

        runTest({ BrowserName: 'Safari', BrowserVersion: '9', OSName: 'Windows XP' }, false, ' (WinXP)');
        runTest({ BrowserName: 'Safari', BrowserVersion: '7', OSName: 'MacOS Sierra' }, true, ' (MacOS Sierra)');
        runTest({
            BrowserName: 'Safari',
            BrowserVersion: '7',
            OSFamily: 'MacOS',
            OSVersion: '10.12.4'
        }, true, ' (MacOS 10.12.4)');
    });
    describe('Chrome', () => {
        runTest({ BrowserName: 'Chrome', BrowserVersion: '0' }, true);
        runTest({ BrowserName: 'Chrome', BrowserVersion: '100' }, true);
    });
    describe('Edge', () => {
        runTest({ BrowserName: 'Edge', BrowserVersion: '14' }, true);
    });
    describe('Incorrect uatraits', () => {
        runTest({ BrowserName: 'MSIE' }, true);
        runTest({ BrowserName: 'MSIE', BrowserVersion: 10 }, false);
        runTest({ BrowserName: 'MSIE', BrowserVersion: 11 }, true);
        runTest({ BrowserName: 'MSIE', BrowserVersion: 'qwerty' }, false);
        runTest({ BrowserName: 'MSIE', BrowserVersion: null }, false);

        runTest({ BrowserName: 'Safari' }, true);
        runTest({ BrowserName: 'Safari', OSName: null }, true, ' (OSName: null)');
    });

    describe('Chromium-Based', () => {
        it('Chromium-based 55.0.1234.15', () => {
            expect(isBrowserSupported(
                { BrowserBase: 'Chromium', BrowserBaseVersion: '55.0.1234.15' },
                [{ BrowserBase: 'Chromium', BrowserBaseVersion: '<56' }]
            )).toEqual(false);
        });
        it('Chromium-based 56.0.2924.87', () => {
            expect(isBrowserSupported(
                { BrowserBase: 'Chromium', BrowserBaseVersion: '56.0.2924.87' },
                [{ BrowserBase: 'Chromium', BrowserBaseVersion: '<56' }]
            )).toEqual(true);
        });
    });

    describe('Trident engine', () => {
        it('Trident engine 6.5', () => {
            expect(isBrowserSupported(
                { BrowserEngine: 'Trident', BrowserEngineVersion: '6.5' },
                [{ BrowserEngine: 'Trident', BrowserEngineVersion: '<7' }]
            )).toEqual(false);
        });
        it('Trident engine 7.0', () => {
            expect(isBrowserSupported(
                { BrowserEngine: 'Trident', BrowserEngineVersion: '7.0' },
                [{ BrowserEngine: 'Chromium', BrowserEngineVersion: '<7' }]
            )).toEqual(true);
        });
    });

    describe('Full real uatraits', () => {
        // собраны здесь скорее для того. чтобы перед глазами был пример реальных uatraits
        runTest({
            BrowserBase: 'Chromium',
            BrowserBaseVersion: '56.0.2924.87',
            BrowserEngine: 'WebKit',
            BrowserEngineVersion: '537.36',
            BrowserName: 'YandexBrowser',
            BrowserVersion: '17.3.1.838',
            OSFamily: 'MacOS',
            OSVersion: '10.12.4',
            YaGUI: '2.5',
            isBrowser: true,
            isMobile: false
        }, true);
        runTest({
            BrowserEngine: 'Trident',
            BrowserEngineVersion: '7.0',
            BrowserName: 'MSIE',
            BrowserVersion: '11.0',
            OSFamily: 'Windows',
            OSName: 'Windows 7',
            OSVersion: '6.1',
            isBrowser: true,
            isMobile: false,
            x64: true
        }, true);
    });
});
