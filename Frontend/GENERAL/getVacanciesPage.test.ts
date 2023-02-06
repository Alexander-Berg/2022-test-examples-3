import { getVacanciesPage, GetVacanciesPageParams } from './getVacanciesPage';
import { updateFiltersWrapper as updateFiltersWrapperOrigin, IFiltersWrapperUpdate } from './updateFiltersWrapper';
import { extend as extendOrigin } from './extend';

import { getFilters } from '../stubs/femidaFilterForm';
import { filtersWrapperSection } from '../stubs/lpcFiltersWrapperSection';
import { getPublications } from '../stubs/femidaPublications';

jest.mock('./updateFiltersWrapper');
jest.mock('./extend');

const updateFiltersWrapper = updateFiltersWrapperOrigin as jest.Mock<
    ReturnType<typeof updateFiltersWrapperOrigin>,
    [IFiltersWrapperUpdate]
>;
const extend = extendOrigin as jest.Mock<
    ReturnType<typeof extendOrigin>
>;

describe('getVacanciesPage', () => {
    let params: GetVacanciesPageParams;

    beforeEach(() => {
        params = {
            filters: getFilters(),
            initialFilters: {
                skills: ['JavaScript', 'TypeScript'],
            },
            lpcJson: filtersWrapperSection,
            publicationsResult: {
                next: 'http://femida.yandex-team.ru:443/_api/jobs/publications/?cursor=bz0yJnA9MTA%3D&page_size=3',
                results: getPublications(),
                count: 1280,
            },
            searchQuery: {
                text: 'search text',
            },
        };
    });

    it('should use extend with updates from updateFiltersWrapper', () => {
        getVacanciesPage(params);

        expect(updateFiltersWrapper).toBeCalledWith({
            filters: params.filters,
            initialFilters: params.initialFilters,
            publications: params.publicationsResult?.results,
            publicationsResult: params.publicationsResult,
            searchString: params.searchQuery.text,
        });

        const updateFiltersWrapperResult = updateFiltersWrapper.mock.results[0].value;

        expect(extend).toBeCalledWith(
            params.lpcJson,
            [updateFiltersWrapperResult],
        );
    });
});
