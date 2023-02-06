'use strict';

const sinon = require('sinon');

const isYclidValid = require('../../../src/models/RequestModel/is-yclid-valid');

describe('Check yclid', () => {
    test('should return true if yclid is valid', () => {
        const dateNowStub = sinon.stub(Date, 'now', () => {
            return 1480680301560; /** Fri Dec 02 2016 15:05:01 GMT+0300 (MSK) */
        });

        const dateGetFullYear = sinon.stub(Date.prototype, 'getFullYear', () => {
            return 2016;
        });

        const VALID_YCLIDS = [
            '7187352651193388087',
            '7187359674969623671',
            '7187366594747832876',
            '7188007529571156963',
            '7188250399044798703',
            '7189805835687300057',
            '7189817756054198790',
            '7189886158908955186',
            '7189886431514335826'
        ];

        VALID_YCLIDS.forEach((yclid) => {
            expect(isYclidValid(yclid)).toBeTruthy();
        });

        dateNowStub.restore();
        dateGetFullYear.restore();
    });

    test('should return false if yclid is not valid', () => {
        const dateNowStub = sinon.stub(Date, 'now', () => {
            return 1484946000000; // Sat Jan 21 2017 00:00:00 GMT+0300 (MSK)
        });

        const dateGetFullYear = sinon.stub(Date.prototype, 'getFullYear', () => {
            return 2016;
        });

        const NOT_VALID_YCLID = [
            7187352651193388087 /** <ymclid> is not instance of string */,
            '718735265119338808' /** <yclid>.length < 19                */,
            '71873526511933880876' /** <yclid>.length > 19                */,
            '718-735265119338808' /** <yclid> is not integer value       */,
            '7187352651193388087',
            '7187359674969623671',
            '7187366594747832876',
            '7188007529571156963',
            '7188250399044798703',
            '7189805835687300057',
            '7189817756054198790',
            '7189886158908955186',
            '7189886431514335826',
            '0000000000000000000',
            '9999999999999999999',
            '6866866866866866868'
        ];

        NOT_VALID_YCLID.forEach((yclid) => {
            expect(isYclidValid(yclid)).toBeFalsy();
        });

        dateNowStub.restore();
        dateGetFullYear.restore();
    });
});
