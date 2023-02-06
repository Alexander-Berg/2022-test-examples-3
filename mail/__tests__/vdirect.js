'use strict';

const mockData = {
    keys: 'c:key\na:b\nqwe:key\n',
    sms: '[{"key":"1qaz2wsx3edc4rfv","iv":"5tgb6yhn","alg":"blowfish"}]'
};

jest.mock('fs');
const fs = require('fs');
jest.spyOn(fs, 'readFileSync').mockImplementation((x) => mockData[x]);

const vdirect = require('../vdirect.js');
const {
    createHashForUidLink,
    validateHashForUidLink,
    validateHashForSmsLink
} = vdirect('keys', 'sms');

describe('createHashForUidLink', () => {
    it('ok', () => {
        const hash = createHashForUidLink('uid', 'link');
        expect(hash).toBe('c,ps9QutsLIYK9FXXLLAGxJg');
    });
});

describe('validateHashForUidLink', () => {
    it('ok', () => {
        const result = validateHashForUidLink('uid', 'link', 'qwe,ps9QutsLIYK9FXXLLAGxJg');
        expect(result).toBe(true);
    });

    it('fail invalid hash', () => {
        const result = validateHashForUidLink('uid', 'link', 'c,ps9QutsLIYK9FXXLLAGxJ_');
        expect(result).toBe(false);
    });

    it('fail non-existent key', () => {
        const result = validateHashForUidLink('uid', 'link', 'oops,ps9QutsLIYK9FXXLLAGxJg');
        expect(result).toBe(false);
    });

    it('fail on hash for different url', () => {
        const hash = createHashForUidLink('uid', 'link+link');
        const result = validateHashForUidLink('uid', 'link', hash);
        expect(result).toBe(false);
    });

    it('fail on hash for different uid', () => {
        const hash = createHashForUidLink('uid+uid', 'link');
        const result = validateHashForUidLink('uid', 'link', hash);
        expect(result).toBe(false);
    });
});

describe('validateHashForSmsLink', () => {
    const smsSid = 'h3yi5jEB+cb4D9uT9xIJv2A6w4TR3TLk9xrm8e9PncEDbS3ha4zn3yXgosml+19oC0etDeTcKI0=';
    const key = 'c';
    const hash = 'a3MOy5AcOM9qBBTsYryq1Q';

    it('ok', () => {
        const result = validateHashForSmsLink('link', `${key},${smsSid},${hash}`, 1000000000);
        expect(result).toBe(true);
    });

    it('fail timestamp', () => {
        const result = validateHashForSmsLink('link', `${key},${smsSid},${hash}`, 1);
        expect(result).toBe(false);
    });

    it('fail no key', () => {
        const result = validateHashForSmsLink('link', 'asd', 1);
        expect(result).toBe(false);
    });

    it('fail hash', () => {
        const result = validateHashForSmsLink('link', 'c,smsSid,hash', 1);
        expect(result).toBe(false);
    });

    it('fail decrypt', () => {
        const result = validateHashForSmsLink('link', `${key},x${smsSid},4Cve1ZDO3GN8a2bCYR1itA`, 1);
        expect(result).toBe(false);
    });
});
