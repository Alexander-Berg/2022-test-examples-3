import { IVacancyMetaItem } from '@yandex-turbo/components/LcJobsVacancy/LcJobsVacancy.types';

import { getVacancyMetaItemLink } from '../LcJobsLink.utils';

const META_ITEM_TYPE = 'some-type';

describe('LcJobsLink.utils', () => {
    describe('getVacancyMetaItemLink', () => {
        it('should return empty string when item param is string', () => {
            expect(getVacancyMetaItemLink(META_ITEM_TYPE, 'some-string')).toEqual('');
        });

        it('should return empty string when item param has no slug', () => {
            const itemMock: IVacancyMetaItem = {
                title: 'some-title',
            };

            expect(getVacancyMetaItemLink(META_ITEM_TYPE, itemMock)).toEqual('');
        });

        it('should return url with search param type-slug', () => {
            const itemMock: IVacancyMetaItem = {
                title: 'some-title',
                slug: 'some-slug',
            };

            expect(getVacancyMetaItemLink(META_ITEM_TYPE, itemMock)).toEqual(`/jobs/vacancies?${META_ITEM_TYPE}=${itemMock.slug}`);
        });

        it('should return empty string when no one item in array has slug', () => {
            const itemMock: IVacancyMetaItem = {
                title: 'some-title',
            };

            expect(getVacancyMetaItemLink(META_ITEM_TYPE, [itemMock, itemMock])).toEqual('');
        });

        it('should return url with search param type-slug when got array with one param', () => {
            const itemMock: IVacancyMetaItem = {
                title: 'some-title',
                slug: 'some-slug',
            };

            expect(getVacancyMetaItemLink(META_ITEM_TYPE, [itemMock])).toEqual(`/jobs/vacancies?${META_ITEM_TYPE}=${itemMock.slug}`);
        });

        it('should return url with two search params when got array with two param', () => {
            const itemMock: IVacancyMetaItem = {
                title: 'some-title',
                slug: 'some-slug',
            };
            const anotherItemMock: IVacancyMetaItem = {
                title: 'some-title-2',
                slug: 'some-slug-2',
            };

            expect(getVacancyMetaItemLink(META_ITEM_TYPE, [itemMock, anotherItemMock]))
                .toEqual(`/jobs/vacancies?${META_ITEM_TYPE}=${itemMock.slug}&${META_ITEM_TYPE}=${anotherItemMock.slug}`);
        });

        it('should return url with search param type-slug when got array with two param one of which is has no slug', () => {
            const itemMock: IVacancyMetaItem = {
                title: 'some-title',
                slug: 'some-slug',
            };
            const anotherItemMock: IVacancyMetaItem = {
                title: 'some-title-2',
            };

            expect(getVacancyMetaItemLink(META_ITEM_TYPE, [itemMock, anotherItemMock])).toEqual(`/jobs/vacancies?${META_ITEM_TYPE}=${itemMock.slug}`);
        });
    });
});
