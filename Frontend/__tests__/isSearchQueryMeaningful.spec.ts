import { KnownObjectType } from '../../config/domain';
import { isSearchQueryMeaningful } from '../query/isSearchQueryMeaningful';

const rightQueryExample: {
    type: KnownObjectType[],
    object_id: string;
} = {
    type: ['banner'],
    object_id: '1',
};

describe('utils/isSearchQueryMeaningful', () => {
    it('meaningful', () => {
        expect(isSearchQueryMeaningful(rightQueryExample)).toBe(true);
    });
});
