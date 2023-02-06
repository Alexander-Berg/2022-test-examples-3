import { getSearchQuery } from './getSearchQuery';

describe('getSearchQuery', () => {
    it('should extract text query parameter if it is passed one value in query', () => {
        expect(getSearchQuery({
            text: 'value',
        })).toStrictEqual({
            text: 'value',
        });
    });

    it('should extract text query parameter if it is passed multiple values in query', () => {
        expect(getSearchQuery({
            text: ['first', 'second'],
        })).toStrictEqual({
            text: 'first',
        });
    });

    it('should extract text query parameter if it is not passed value in query', () => {
        expect(getSearchQuery({})).toStrictEqual({});
    });

    it('should ignore other keys/values in query', () => {
        expect(getSearchQuery({
            key1: 'value1',
            key2: 'value2',
        })).toStrictEqual({});
    });
});
