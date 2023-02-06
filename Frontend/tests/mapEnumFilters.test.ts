import uniqBy from 'lodash/uniqBy';
import cloneDeep from 'lodash/cloneDeep';
import sortBy from 'lodash/sortBy';

import type { ITemplateFilterValue } from '@src/schema/search/types';
import { mergeEnumFilterValues } from '..';

const DUPLICATE_SHOP_NAME = 'Ситилинк';
const VALID_ID = '1993907,697633,773486';

const filtersStub: ITemplateFilterValue[] = [
    { value: DUPLICATE_SHOP_NAME, found: 199, id: '773486' },
    { value: 'Lemmex.ru', found: 4908, id: '892153' },
    { value: 'MentalShop.ru', found: 192, id: '712423' },
    { value: DUPLICATE_SHOP_NAME, found: 376, id: '697633' },
    { value: 'Print Bar', found: 465, id: '921438' },
    { value: 'RusExpress', found: 240, id: '857293' },
    { value: 'Super01', found: 179, id: '919186' },
    { value: DUPLICATE_SHOP_NAME, found: 145, id: '1993907' },
    { value: 'Аксессуары из меха', found: 4120, id: '1744216' },
    { value: 'Дом Подарка', found: 275, id: '779271' },
    { value: 'Магазин головных уборов - Пильников', found: 903, id: '752330' },
    { value: 'Русские кепки', found: 490, id: '921900' },
];

describe('mergeEnumFilterValues', () => {
    it('должен отфильтровать магазины без названия', () => {
        const filters = cloneDeep(filtersStub.slice(0, 3));
        filters[0].value = '';
        filters[1].value = '';

        const result = mergeEnumFilterValues(filters);

        expect(result.length).toEqual(filters.length - 2);
    });

    it('должен объединить дублирующиеся фильтры', () => {
        const filters = cloneDeep(filtersStub);
        const result = mergeEnumFilterValues(filters);

        const uniqueShops = uniqBy(result, 'value');
        expect(uniqueShops.length).toEqual(filters.length - 2);
    });

    it('должен склеить id дублирующихся фильтров', () => {
        const filters = cloneDeep(filtersStub);
        const result = mergeEnumFilterValues(filters);

        const filter = result.find(({ value }) => value === DUPLICATE_SHOP_NAME);

        expect(filter).not.toBeUndefined();
        expect(filter?.id).toEqual(VALID_ID);
    });

    it('должен склеить id дублирующихся фильтров в одинаковую строку, независимо от порядка фильтров', () => {
        const filters = cloneDeep(filtersStub);

        const sortedFilters = sortBy(filters, 'found');
        const result = mergeEnumFilterValues(sortedFilters);

        const filter = result.find(({ value }) => value === DUPLICATE_SHOP_NAME);

        expect(filter).not.toBeUndefined();
        expect(filter?.id).toEqual(VALID_ID);
    });

    it('должен выставить checked всем дублирующимся фильтрам, если хотя бы один из них checked', () => {
        const filters = cloneDeep(filtersStub);

        filters[0].checked = true;
        const result = mergeEnumFilterValues(filters);

        const filter = result.find(({ value }) => value === DUPLICATE_SHOP_NAME);

        expect(filter).not.toBeUndefined();
        expect(filter?.checked).toEqual(true);
    });
});
