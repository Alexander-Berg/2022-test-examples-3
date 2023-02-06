/* eslint-disable no-unused-expressions */

'use strict';

const isValidBrowser = require('../../lib/helper.js').isValidBrowser;
const config = require('cfg');

const { expect } = require('chai');

describe('isValidBrowser', () => {
    it('should return true for exact name and version', function () {
        const uatraits = { BrowserName: 'opera', BrowserVersion: '59' };
        const isValid = isValidBrowser(uatraits, config.proctoringSupportBrowsers);

        expect(isValid).to.be.true;
    });

    it('should return true for not exact name and correct version', function () {
        const uatraits = { BrowserName: 'YandexBrowser', BrowserVersion: '19.3.1.768' };
        const isValid = isValidBrowser(uatraits, config.proctoringSupportBrowsers);

        expect(isValid).to.be.true;
    });

    it('should return false for exact name and version below required', function () {
        const uatraits = { BrowserName: 'opera', BrowserVersion: '15.6.1.768' };
        const isValid = isValidBrowser(uatraits, config.proctoringSupportBrowsers);

        expect(isValid).to.be.false;
    });

    it('should return true for browser version without dot', function () {
        const uatraits = { BrowserName: 'YandexBrowser', BrowserVersion: '20' };
        const isValid = isValidBrowser(uatraits, config.proctoringSupportBrowsers);

        expect(isValid).to.be.true;
    });

    it('should return true for browser which version is less after dot', function () {
        const uatraits = { BrowserName: 'yandex', BrowserVersion: '20.2' };
        const isValid = isValidBrowser(uatraits, config.proctoringSupportBrowsers);

        expect(isValid).to.be.true;
    });

    it('should return false for not exact name and version below required after dot', function () {
        const uatraits = { BrowserName: 'YandexBrowser', BrowserVersion: '19.0.3' };
        const isValid = isValidBrowser(uatraits, config.proctoringSupportBrowsers);

        expect(isValid).to.be.false;
    });

    it('should return false for browser which not supports proctoring', function () {
        const uatraits = { BrowserName: 'NoProctoringBrowser', BrowserVersion: '16.6.1.768' };
        const isValid = isValidBrowser(uatraits, config.proctoringSupportBrowsers);

        expect(isValid).to.be.false;
    });
});
