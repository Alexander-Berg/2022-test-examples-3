import { getVasVersion } from './index';

describe('getVasVersion', () => {
    it('should return adsdkver from query as first priority', () => {
        expect(getVasVersion({
            adsdkver: '1',
        }, {
            ADSDKVER: '2',
            VAS_STABLE_VERSION: '3',
        })).toBe('1');
    });

    it('should return ADSDKVER from AB as second priority', () => {
        expect(getVasVersion({}, {
            ADSDKVER: '2',
            VAS_STABLE_VERSION: '3',
        })).toBe('2');
    });

    it('should return VAS_STABLE_VERSION from AB as third priority', () => {
        expect(getVasVersion({}, {
            VAS_STABLE_VERSION: '3',
        })).toBe('3');
    });

    it('should return 0 by default', () => {
        expect(getVasVersion({}, {})).toBe('0');
    });

    it('should return correct value from array-like query', () => {
        expect(getVasVersion({ adsdkver: ['33', '1'] }, {
            ADSDKVER: '2',
            VAS_STABLE_VERSION: '3',
        })).toBe('33');
    });
});
