'use strict';

const sinon = require('sinon');

const isYmclidValid = require('../../../src/models/RequestModel/is-ymclid-valid');

describe('Check ymclid', () => {
    let dateNowStub;
    let dateGetFullYear;

    beforeAll(() => {
        dateNowStub = sinon.stub(Date, 'now', () => {
            return 1480658716576;
        });

        dateGetFullYear = sinon.stub(Date.prototype, 'getFullYear', () => {
            return 2016;
        });
    });

    afterAll(() => {
        dateNowStub && dateNowStub.restore();
        dateGetFullYear && dateGetFullYear.restore();
    });

    test('should return true if ymclid is valid', () => {
        const VALID_YMClIDS = [
            '14806565936984426184100002',
            '14806565936984426184100001',
            '14806567060612627822900001',
            '14806567060612627822900005'
        ];

        VALID_YMClIDS.forEach((ymclid) => {
            expect(isYmclidValid(ymclid)).toBeTruthy();
        });
    });

    test('should return false is ymclid is not valid', () => {
        const NOT_VALID_YMCLIDS = [
            '80656593698442618410000' /** <ymclid>.length < 26                              */,
            '148065659369844261841000023' /** <ymclid>.length > 26                              */,
            '14806-56593698442618410002' /** <ymclid> is not integer value                     */,
            14806565936984426184100002 /** <ymclid> is not instance of string                */,
            '14806587165774426184100002' /** <Time part> = <Time of the occurrence of now> + 1 */
        ];

        NOT_VALID_YMCLIDS.forEach((ymclid) => {
            expect(isYmclidValid(ymclid)).toBeFalsy();
        });
    });
});
