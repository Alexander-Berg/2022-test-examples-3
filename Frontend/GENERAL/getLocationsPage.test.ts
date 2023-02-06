import { groupItems, simplifyCityServices, transformCityToEntityListItem, getLocationsPage } from './getLocationsPage';

import { IServicesListItemCity, ICitiesListItem, IListWithPagination } from '../../typings/femida';
import { ILcRootTurboJSON, LcJobsEntityListItem } from '../../typings/lpc';

import { getTruncatedCitiesItems, getCities } from '../stubs/femidaCities';
import { getEntityListSection } from '../stubs/lpcEntityListSection';

const getError = (result: ILcRootTurboJSON) => {
    // @ts-ignore
    return result.content[0].content[0].error as string;
};

describe('getLocationsPage', () => {
    let entityListSection: ILcRootTurboJSON;
    let citiesListItems: ICitiesListItem[];
    let cities: IListWithPagination<ICitiesListItem>;

    const expectedCityItems: LcJobsEntityListItem[] = [
        {
            name: 'Гродно',
            publicationsCount: 2,
            group: {
                title: 'Белоруссия',
                data: {
                    countryCode: 'BY',
                    description: undefined,
                    position: undefined,
                    slug: undefined,
                },
            },
            services: [{ name: 'Яндекс Go', slug: 'go', priority: 2 }],
            slug: 'grodno',
        },
        {
            name: 'Москва',
            publicationsCount: 123,
            group: {
                title: 'Россия',
                data: {
                    countryCode: 'RU',
                    description: 'some description',
                    position: 123,
                    slug: 'russia',
                },
            },
            services: [],
            slug: 'moscow',
        },
    ];

    beforeEach(() => {
        entityListSection = getEntityListSection();
        citiesListItems = getTruncatedCitiesItems();
        cities = getCities();
    });

    describe('simplifyCityServices', () => {
        let cityServices: IServicesListItemCity[];

        beforeEach(() => {
            cityServices = [
                {
                    id: 1,
                    name: 'Яндекс Go',
                    slug: 'go',
                    priority: 2,
                },
                {
                    id: 34,
                    name: 'Интранет',
                    slug: 'intranet',
                    priority: 1,
                },
                {
                    id: 35,
                    name: 'Маркет',
                    slug: 'market',
                    priority: 3,
                },
                {
                    id: 36,
                    name: 'Облако',
                    slug: 'cloud',
                    priority: 2,
                },
            ];
        });

        it('should preserve only needed fields of services', () => {
            expect(simplifyCityServices(cityServices)).toStrictEqual([
                { name: 'Яндекс Go', slug: 'go', priority: 2 },
                { name: 'Интранет', slug: 'intranet', priority: 1 },
                { name: 'Маркет', slug: 'market', priority: 3 },
                { name: 'Облако', slug: 'cloud', priority: 2 },
            ]);
        });

        it('should preserve empty array if passed that', () => {
            expect(simplifyCityServices([])).toEqual([]);
        });
    });

    describe('transformCityToEntityListItem', () => {
        it('should properly handle city entity', () => {
            expect(transformCityToEntityListItem(citiesListItems[0])).toStrictEqual(expectedCityItems[0]);
        });

        it('should extract optional parameters: description, position and slug from country of city', () => {
            expect(transformCityToEntityListItem(citiesListItems[1])).toStrictEqual(expectedCityItems[1]);
        });
    });

    describe('groupItems', () => {
        it('should handle each city item', () => {
            expect(groupItems(citiesListItems)).toStrictEqual(expectedCityItems);
        });

        it('should properly handle invoking without params', () => {
            expect(groupItems()).toEqual([]);
        });
    });

    it('should add error if cities is not passed', () => {
        const results = [
            getLocationsPage(entityListSection),
            getLocationsPage(entityListSection, {} as IListWithPagination<ICitiesListItem>),
        ];

        for (const result of results) {
            const error = getError(result);

            // Ensure, that we have that error field in result.
            expect(typeof error).toBe('string');
        }
    });

    it('should correct extend entity list section with cities', () => {
        expect(getLocationsPage(entityListSection, cities)).toMatchSnapshot();
    });
});
