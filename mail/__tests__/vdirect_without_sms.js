'use strict';

const mockData = {
    keys: 'c:key\na:b\nqwe:key\n'
};

jest.mock('fs');
const fs = require('fs');
jest.spyOn(fs, 'readFileSync').mockImplementation((x) => mockData[x]);

const vdirect = require('../vdirect.js');
const vdirect_object = vdirect('keys');

describe('withoutSms', () => {
    it('ok', () => {
        expect(vdirect_object).not.toHaveProperty('validateHashForSmsLink');
    });
});
