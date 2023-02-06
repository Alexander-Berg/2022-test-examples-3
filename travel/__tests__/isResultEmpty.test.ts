import {SEARCH} from '../../../routes/search';

import SearchSegment from '../../../interfaces/state/search/SearchSegment';
import IStatePage from '../../../interfaces/state/IStatePage';

import isResultEmpty from '../isResultEmpty';

const page = {fetching: null} as IStatePage;
const segments = [] as SearchSegment[];

describe('isResultEmpty', () => {
    it('Если данные для страницы еще не получены - вернёт false', () => {
        const testCaseParams = {
            segments,
            page: {
                fetching: SEARCH,
            } as IStatePage,
        };

        expect(isResultEmpty(testCaseParams)).toBe(false);
    });

    it('Если список сегментов не пуст - вернёт false', () => {
        const testCaseParams = {
            page,
            segments: [{}] as SearchSegment[],
        };

        expect(isResultEmpty(testCaseParams)).toBe(false);
    });

    it('Если данные для страницы получены, но список сегментов пуст - венрёт true', () => {
        expect(isResultEmpty({page, segments})).toBe(true);
    });
});
