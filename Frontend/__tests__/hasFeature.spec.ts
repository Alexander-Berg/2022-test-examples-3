import { hasFeature } from '../hasFeature';

describe('utils/addHttps', () => {
    it('has feature', () => {
        expect(hasFeature('direct', 'custom')).toBe(true);
    });
});
