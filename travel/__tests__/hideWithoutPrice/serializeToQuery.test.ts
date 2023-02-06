import hideWithoutPrice from '../../hideWithoutPrice';

describe('hideWithoutPrice.serializeToQuery', () => {
    it('show segments with price only', () => {
        const query = hideWithoutPrice.serializeToQuery(true);

        expect(query).toEqual({seats: 'y'});
    });

    it('show all segments', () => {
        const query = hideWithoutPrice.serializeToQuery(false);

        expect(query).toEqual({});
    });
});
