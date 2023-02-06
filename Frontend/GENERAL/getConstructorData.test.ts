import { getConstructorData } from './getConstructorData';

describe('[VASTAdCreator] getConstructorData', () => {
    it('should return correct value', () => {
        expect(getConstructorData({ Theme: 'theme' })).toEqual({ Theme: 'theme' });
        expect(getConstructorData({ Video: { Theme: 'theme' } })).toEqual({ Theme: 'theme' });
        expect(getConstructorData(undefined)).toEqual(undefined);
    });
});
