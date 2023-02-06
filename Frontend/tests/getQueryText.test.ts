import { assert } from 'chai';
import { getQueryText } from '../.';

describe('Query text getter', () => {
    it('should return query text', () => {
        assert.equal(getQueryText({ query: { text: 'text' } }), 'text');
    });

    it('should able to cut text', () => {
        assert.equal(getQueryText({
            query: { text: 'отель белград смотреть онлайн' },
        }, 16), 'отель белград см...');
        assert.equal(getQueryText({ query: { text: 'text' } }, 16), 'text');
    });
});
