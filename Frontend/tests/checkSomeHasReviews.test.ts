import { assert } from 'chai';
import { checkSomeHasReviews } from '../.';

describe('checkSomeHasReviews', () => {
    it('should return true', () => {
        assert.isTrue(checkSomeHasReviews([{}, {}, { reviews: {} }]));

        assert.isTrue(checkSomeHasReviews([{ reviews: {} }]));
    });

    it('should return false', () => {
        assert.isFalse(checkSomeHasReviews([]));

        assert.isFalse(checkSomeHasReviews([{}, {}]));
    });
});
