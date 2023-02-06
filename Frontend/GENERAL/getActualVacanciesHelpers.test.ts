import { getActualVacancies, getShowAllUrl } from './getActualVacanciesHelpers';
import { VACANCIES_MAX_LENGTH } from '../../constants';

import { IPublication } from '../../typings/femida';

import { getPublications } from '../stubs/femidaPublications';
import { mapPageToUrlPath } from './updateActualVacancies';

describe('getActualVacanciesHelpers', () => {
    describe('getActualVacancies()', () => {
        let stub: IPublication[];

        beforeEach(() => {
            stub = getPublications();
        });

        it('should return 7 vacancies (or less)', () => {
            expect(getActualVacancies(stub, mapPageToUrlPath.service)!).toHaveLength(VACANCIES_MAX_LENGTH); // eslint-disable-line
        });

        it('should return null if no vacancies', () => {
            expect(getActualVacancies([], mapPageToUrlPath.service)).toBe(null);
        });

        it('should return appropriate data for service page', () => {
            expect(getActualVacancies(stub.slice(0, 2), mapPageToUrlPath.service)).toEqual([
                {
                    id: 5474,
                    cities: [
                        {
                            name: 'Москва',
                            slug: 'moscow'
                        }
                    ],
                    service: null,
                    title: 'Координатор в Маркет',
                },
                {
                    id: 5205,
                    title: 'Ведущий дизайнер продукта в Маркет',
                    cities: [
                        {
                            name: 'Москва',
                            slug: 'moscow'
                        }
                    ],
                    service: null,
                },
            ]);
        });

        it('should return appropriate data for profession pages', () => {
            expect(getActualVacancies(stub.slice(0, 2), mapPageToUrlPath.profession)).toEqual([
                {
                    id: 5474,
                    cities: [],
                    service: {
                        name: 'Маркет',
                        slug: 'market'
                    },
                    title: 'Координатор в Маркет',
                },
                {
                    id: 5205,
                    title: 'Ведущий дизайнер продукта в Маркет',
                    cities: [],
                    service: {
                        name: 'Маркет',
                        slug: 'market'
                    },
                },
            ]);
        });

        it('should return appropriate data for location pages', () => {
            expect(getActualVacancies(stub.slice(0, 2), mapPageToUrlPath.location)).toEqual([
                {
                    id: 5474,
                    cities: [],
                    service: {
                        name: 'Маркет',
                        slug: 'market'
                    },
                    title: 'Координатор в Маркет',
                },
                {
                    id: 5205,
                    title: 'Ведущий дизайнер продукта в Маркет',
                    cities: [],
                    service: {
                        name: 'Маркет',
                        slug: 'market'
                    },
                },
            ]);
        });
    });

    describe('getShowAllUrl()', () => {
        it('should return correct url for service', () => {
            expect(getShowAllUrl(mapPageToUrlPath.service, 'go')).toBe('/jobs/services/go');
        });

        it('should return correct url for cities', () => {
            expect(getShowAllUrl(mapPageToUrlPath.location, 'moscow')).toBe('/jobs/locations/moscow');
        });

        it('should reeturn correct url for professions', () => {
            expect(getShowAllUrl(mapPageToUrlPath.profession, 'backend-developer')).toBe('/jobs/professions/backend-developer');
        });
    });
});
