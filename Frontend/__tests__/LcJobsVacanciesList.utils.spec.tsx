import { IPublication, ICity, ISkill, IPublicationService, IFiltersValue } from '@yandex-turbo/types/jobs';

import { getMappedVacancies, getVacancyMetaItem, prepareTags } from '../LcJobsVacanciesList.utils';

const cityMock: ICity = {
    name: 'some-city-1',
    slug: 'some-city-slug-1',
};

const skillMock: ISkill = {
    name: 'some-skill-1',
    id: 123,
};

const publicServiceMock: IPublicationService = {
    name: 'some-publication',
    slug: 'some-publication-slug',
    id: 123,
    is_active: true,
};

const publicationMock: IPublication = {
    public_service: {
        ...publicServiceMock,
    },
    vacancy: {
        cities: [
            { ...cityMock },
            {
                name: 'some-city-2',
                slug: 'some-city-slug-2',
            }
        ],
        skills: [
            { ...skillMock },
            {
                name: 'some-skill-2',
                id: 1235444,
            }
        ]
    }
};

describe('LcJobsVacanciesList.utils', () => {
    describe('prepareTags', () => {
        let cities: ICity[];
        let skills: ISkill[];
        let filters: IFiltersValue;

        beforeEach(() => {
            cities = [
                {
                    name: 'some-city-1',
                    slug: 'some-city-slug-1',
                },
                {
                    name: 'some-city-2',
                    slug: 'some-city-slug-2',
                },
                {
                    name: 'some-city-3',
                    slug: 'some-city-slug-3',
                },
            ];

            skills = [
                {
                    name: 'Python',
                    id: 1,
                },
                {
                    name: 'JavaScript',
                    id: 2,
                },
                {
                    name: 'ML',
                    id: 3,
                },
            ];

            filters = {
                cities: ['some-city-slug-2'],
                skills: [2]
            };
        });

        it('should sort cities by being in filters', () => {
            const actual = prepareTags(cities, filters.cities, 'slug');

            expect(actual[0]).toHaveProperty('slug', 'some-city-slug-2');
            expect(actual.slice(1)).toEqual(expect.arrayContaining([
                {
                    title: 'some-city-1',
                    slug: 'some-city-slug-1',
                },
                {
                    title: 'some-city-3',
                    slug: 'some-city-slug-3',
                },
            ]));
        });

        it('should sort cities by name after sorting by being in filters', () => {
            const actual = prepareTags(cities, ['some-city-slug-3', 'some-city-slug-2'], 'slug');

            expect(actual).toEqual([
                {
                    title: 'some-city-2',
                    slug: 'some-city-slug-2',
                },
                {
                    title: 'some-city-3',
                    slug: 'some-city-slug-3',
                },
                {
                    title: 'some-city-1',
                    slug: 'some-city-slug-1',
                },
            ]);
        });

        it('should sort cities by name after sorting by absenting in filters', () => {
            const actual = prepareTags(cities, [], 'slug');

            expect(actual).toEqual([
                {
                    title: 'some-city-1',
                    slug: 'some-city-slug-1',
                },
                {
                    title: 'some-city-2',
                    slug: 'some-city-slug-2',
                },
                {
                    title: 'some-city-3',
                    slug: 'some-city-slug-3',
                },
            ]);
        });

        it('should sort cities by slug after sorting by name', () => {
            const cities = [
                {
                    name: 'some-city-1',
                    slug: 'some-city-slug-2',
                },
                {
                    name: 'some-city-1',
                    slug: 'some-city-slug-1',
                },
                {
                    name: 'some-city-1',
                    slug: 'some-city-slug-3',
                },
            ];
            const actual = prepareTags(cities, ['some-city-slug-1', 'some-city-slug-3', 'some-city-slug-2'], 'slug');

            expect(actual).toEqual([
                {
                    title: 'some-city-1',
                    slug: 'some-city-slug-1',
                },
                {
                    title: 'some-city-1',
                    slug: 'some-city-slug-2',
                },
                {
                    title: 'some-city-1',
                    slug: 'some-city-slug-3',
                },
            ]);
        });

        it('should sort skills by being in filters', () => {
            const actual = prepareTags(skills, filters.skills, 'id');

            expect(actual[0]).toHaveProperty('slug', '2');
            expect(actual.slice(1)).toEqual(expect.arrayContaining([
                {
                    title: 'Python',
                    slug: '1',
                },
                {
                    title: 'ML',
                    slug: '3',
                },
            ]));
        });

        it('should sort skills by name after sorting by being in filters', () => {
            const actual = prepareTags(skills, ['3', '2'], 'id');

            expect(actual).toEqual([
                {
                    title: 'JavaScript',
                    slug: '2',
                },
                {
                    title: 'ML',
                    slug: '3',
                },
                {
                    title: 'Python',
                    slug: '1',
                },
            ]);
        });

        it('should sort skills by name after sorting by absenting in filters', () => {
            const actual = prepareTags(skills, [], 'id');

            expect(actual).toEqual([
                {
                    title: 'JavaScript',
                    slug: '2',
                },
                {
                    title: 'ML',
                    slug: '3',
                },
                {
                    title: 'Python',
                    slug: '1',
                },
            ]);
        });

        it('should sort skills by slug after sorting by name', () => {
            const skills = [
                {
                    name: 'Python',
                    id: 1,
                },
                {
                    name: 'Python',
                    id: 2,
                },
                {
                    name: 'Python',
                    id: 3,
                },
            ];
            const actual = prepareTags(skills, ['1', '3', '2'], 'id');

            expect(actual).toEqual([
                {
                    title: 'Python',
                    slug: '1',
                },
                {
                    title: 'Python',
                    slug: '2',
                },
                {
                    title: 'Python',
                    slug: '3',
                },
            ]);
        });

        it('should work if got insufficient data', () => {
            const skills = [
                {
                    name: 'Python',
                    id: 1,
                },
                {
                    name: 'JavaScript',
                },
                {
                    id: 2,
                },
                {
                    name: 'ML',
                    id: 3,
                },
            ];
            const actual = prepareTags(skills, ['1', '3', '2'], 'id');

            expect(actual).toEqual([
                {
                    title: '',
                    slug: '2',
                },
                {
                    title: 'ML',
                    slug: '3',
                },
                {
                    title: 'Python',
                    slug: '1',
                },
                {
                    title: 'JavaScript',
                    slug: '',
                },
            ]);
        });
    });

    describe('getMappedVacancies', () => {
        it('should return vacancies data in internal format to display in vacacnies list', () => {
            const filtersValueMock = {};
            expect(getMappedVacancies(filtersValueMock, [publicationMock])).toMatchSnapshot();
        });

        it('should return vacancies data only for current filters', () => {
            const filtersValueMock: IFiltersValue = {
                cities: ['some-city-slug-2'],
                skills: ['1235444']
            };
            expect(getMappedVacancies(filtersValueMock, [publicationMock])).toMatchSnapshot();
        });
    });

    describe('getVacancyMetaItem', () => {
        it('should return meta item from city object', () => {
            expect(getVacancyMetaItem(cityMock)).toEqual({
                title: cityMock.name,
                slug: cityMock.slug,
            });
        });

        it('should return meta item from service object', () => {
            expect(getVacancyMetaItem(publicServiceMock)).toEqual({
                title: publicServiceMock.name,
                slug: publicServiceMock.slug,
            });
        });

        it('should return meta item from skill object', () => {
            expect(getVacancyMetaItem(skillMock)).toEqual({
                title: skillMock.name,
                slug: String(skillMock.id),
            });
        });
    });
});
