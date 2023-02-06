import { prepareSortType } from '../prepareSortType';
import { sortType, sortKeys } from '../../../types/mayak';

describe('prepareSortType', () => {
    it('expected result with ASC sort type', () => {
        expect(prepareSortType({ type: sortType.ASC, key: sortKeys.crawl_date })).toMatchInlineSnapshot('"crawl_date"');
    });

    it('expected result with DESC sort type', () => {
        expect(prepareSortType({ type: sortType.DESC, key: sortKeys.crawl_date })).toMatchInlineSnapshot(
            '"crawl_date-"',
        );
    });
});
