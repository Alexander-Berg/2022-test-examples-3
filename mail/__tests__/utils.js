'use strict';

jest.mock('fs');
const fs = require('fs');

const { calculateHash, getDecrypt, getPair, getKeys } = require('../utils.js');

describe('calculateHash', () => {
    it('ok', () => {
        const hash = calculateHash('secret', 'uid', 'link');
        expect(hash).toBe('u1AKGHgGK9l1_9z0shDWcA');
    });
});

describe('getPair', () => {
    it('ok', () => {
        const pair = getPair('a:b:c', ':');
        expect(pair).toEqual([ 'a', 'b:c' ]);
    });

    it('empty key', () => {
        const pair = getPair(':b:c', ':');
        expect(pair).toEqual([ '', 'b:c' ]);
    });

    it('empty value', () => {
        const pair = getPair('a:', ':');
        expect(pair).toEqual([ 'a', '' ]);
    });

    it('no delimiter', () => {
        const pair = getPair('a:b:c', ',');
        expect(pair).toEqual([ null, null ]);
    });
});

describe('getKeys', () => {
    let mockKeys;

    beforeEach(() => {
        jest.spyOn(fs, 'readFileSync').mockImplementation(() => mockKeys);
    });

    it('getKeys ok', () => {
        mockKeys = 'a:b\nc:key\n';
        const keys = getKeys('keys');
        expect(keys).toBeInstanceOf(Map);
        expect(keys.size).toBe(2);
        expect(keys.get('c')).toEqual('key');
    });

    it('getKeys bad', () => {
        mockKeys = 'a:b\nc';
        expect(() => getKeys('keys')).toThrow('Cannot find delimiter');
    });
});

describe('getDecrypt', () => {
    let mockSecrets;

    beforeEach(() => {
        mockSecrets = '[{"key":"1qaz2wsx3edc4rfv","iv":"5tgb6yhn","alg":"blowfish"}]';
        jest.spyOn(fs, 'readFileSync').mockImplementation(() => mockSecrets);
    });

    it('ok', () => {
        const decrypt = getDecrypt('fake');
        expect(decrypt).toBeInstanceOf(Function);
        const result = decrypt('h3yi5jEB+cb4D9uT9xIJv2A6w4TR3TLk9xrm8e9PncEDbS3ha4zn3yXgosml+19oC0etDeTcKI0=');
        expect(result).toBe('v2,{"uid":100500,"mid":123456789,"ts":1564228800}');
    });

    it('fails on invalid data', () => {
        const decrypt = getDecrypt('fake');
        const result = decrypt('bad');
        expect(result).toBeNull();
    });
});
