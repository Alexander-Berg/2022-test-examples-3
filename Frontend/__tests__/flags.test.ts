import { getFlag, getBinaryFlag } from '../flags';

const flagName = 'theme';
const binaryFlag = 'isWidget';

describe('#getFlag', () => {
    it('returns flag value', () => {
        const value = 'dark';
        window.flags[flagName] = value;

        expect(getFlag(flagName)).toBe(value);
    });

    it('returns undefined for absent flag', () => {
        delete window.flags[flagName];

        expect(getFlag(flagName)).toBeUndefined();
    });
});

describe('#getBinaryFlag', () => {
    it('returns true for binary enabled flag', () => {
        window.flags[binaryFlag] = '1';

        expect(getBinaryFlag(binaryFlag)).toBeTruthy();
    });

    it('returns false for absent binary flag', () => {
        delete window.flags[binaryFlag];

        expect(getBinaryFlag(binaryFlag)).toBeFalsy();
    });
});
