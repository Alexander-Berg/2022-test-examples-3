import { updateFiltersWrapper, IFiltersWrapperUpdate } from './updateFiltersWrapper';
import { mapPageToFacet } from './getWithVacanciesPage';
import { getPublicationsList } from './getPublicationsList';
import { PAGE_SIZE } from '../../constants';

import { ILcRootTurboJSON } from '../../typings/lpc';

import { getPublications } from '../stubs/femidaPublications';
import { getFilters } from '../stubs/femidaFilterForm';

jest.mock('./getPublicationsList');

const DEFAULT_HOST = 'https://yandex.ru';

describe('updateFiltersWrapper', () => {
    let params: IFiltersWrapperUpdate;

    let filtersWrapperSection: ILcRootTurboJSON;

    beforeEach(() => {
        params = {
            filters: getFilters(),
            initialFilters: {
                skills: ['JavaScript', 'TypeScript'],
            },
            publications: getPublications(),
            publicationsResult: {
                next: 'http://femida.yandex-team.ru:443/_api/jobs/publications/?cursor=bz0yJnA9MTA%3D&page_size=3',
                results: getPublications(),
                count: 1280,
            },
        };

        filtersWrapperSection = {
            block: 'lc-jobs-filters-wrapper',
            filters: [],
            vacancies: [],
        } as unknown as ILcRootTurboJSON;
    });

    it('should not add fixed filters if page and id is not defined', () => {
        const { fixedFiltersValue: withoutId } = updateFiltersWrapper({
            ...params, page: 'profession',
        }).updates;

        expect(withoutId).toBeUndefined();

        const { fixedFiltersValue: withoutPage } = updateFiltersWrapper({
            ...params, id: 123,
        }).updates;

        expect(withoutPage).toBeUndefined();

        const { fixedFiltersValue: withoutPageAndId } = updateFiltersWrapper(params).updates;

        expect(withoutPageAndId).toBeUndefined();
    });

    it('should add fixed filters if page and id defined', () => {
        params.id = 123;
        params.page = Object.keys(mapPageToFacet)[0];

        const { fixedFiltersValue } = updateFiltersWrapper(params).updates;

        expect(fixedFiltersValue).toStrictEqual({
            [String(Object.values(mapPageToFacet)[0])]: [params.id],
        });
    });

    it('should add searchString if searchString parameter is defined', () => {
        params.searchString = 'search text';

        const { initialSearchString } = updateFiltersWrapper(params).updates;

        expect(initialSearchString).toBe('search text');
    });

    it('should add filter constraints (fixed filters) in url field if page and id defined', () => {
        params.id = 123;
        params.page = Object.keys(mapPageToFacet)[0];

        const { vacancies } = updateFiltersWrapper(params).updates;

        const { url } = vacancies(filtersWrapperSection);

        const paramsKey = mapPageToFacet[params.page];
        const filterVal = (new URL(url, DEFAULT_HOST)).searchParams.get(paramsKey);
        expect(filterVal).toBe(String(params.id));
    });

    it('should add dictionaries field for passed filters', () => {
        const { filters } = updateFiltersWrapper(params).updates;

        const { dictionaries } = filters(filtersWrapperSection);

        expect(dictionaries).toBe(params.filters);
    });

    it('should add initialValue field for passed initialFilters', () => {
        const { filters } = updateFiltersWrapper(params).updates;

        const { initialValue } = filters(filtersWrapperSection);

        expect(initialValue).toBe(params.initialFilters);
    });

    it('should add nextUrl field for next from publicationResults', () => {
        const next = 'some next url';
        params.publicationsResult = {
            next,
        };

        const { vacancies } = updateFiltersWrapper(params).updates;

        const { nextUrl } = vacancies(filtersWrapperSection);

        expect(nextUrl).toBe(next);
    });

    it('should add url field for request with page size constraints', () => {
        const { vacancies } = updateFiltersWrapper(params).updates;

        const { url } = vacancies(filtersWrapperSection);

        const pageSizeVal = (new URL(url, DEFAULT_HOST)).searchParams.get('page_size');
        expect(pageSizeVal).toBe(String(PAGE_SIZE));
    });

    it('should add count in vacancies', () => {
        const count = 123;
        params.publicationsResult = {
            count,
        };

        const { vacancies } = updateFiltersWrapper(params).updates;

        const { initialCount } = vacancies(filtersWrapperSection);

        expect(initialCount).toBe(count);
    });

    it('should use getPublicationsList for getting publications', () => {
        const { vacancies } = updateFiltersWrapper(params).updates;

        vacancies(filtersWrapperSection);

        expect(getPublicationsList).toBeCalledWith(params.publications);
    });
});
