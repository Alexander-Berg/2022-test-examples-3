import type { IFilters } from '@src/typings/filters';
import { getAppliedAndDisableFilters } from '../../hooks/useAppliedFilters';

describe('Filters/hooks/useAppliedFilters', () => {
    it('Возвращает примененные enum фильтры', () => {
        const defaultOrder = ['brand', 'shop', 'size'];
        const filters: IFilters = {
            brand: {
                type: 'enum',
                title: '',
                id: 'brand',
                param: '',
                values: [
                    { value: '1', found: 1, id: '1' },
                    { value: '2', found: 1, id: '2' },
                    { value: '3', found: 1, id: '3' },
                ],
            },
            shop: {
                type: 'enum',
                title: '',
                id: 'shop',
                param: '',
                values: [
                    { value: '1', found: 1, id: '1' },
                    { value: '2', found: 1, id: '2', checked: true },
                    { value: '3', found: 1, id: '3' },
                ],
            },
            size: {
                type: 'enum',
                title: '',
                id: 'size',
                param: '',
                values: [
                    { value: '1', found: 0, id: '1' },
                    { value: '2', found: 0, id: '2' },
                    { value: '3', found: 0, id: '3' },
                ],
            },
        };
        const { appliedFilters, disabledFilters } = getAppliedAndDisableFilters(defaultOrder, filters);

        expect(appliedFilters.shop).toBeDefined();
        expect(disabledFilters.size).toBeDefined();
    });

    it('Возвращает примененные boolean фильтры', () => {
        const defaultOrder = ['brand', 'shop', 'size'];
        const filters: IFilters = {
            brand: {
                type: 'boolean',
                title: '',
                id: 'brand',
                param: '',
                values: [{ value: '1', found: 0, id: '1' }],
            },
            shop: {
                type: 'boolean',
                title: '',
                id: 'shop',
                param: '',
                values: [{ value: '1', found: 1, id: '1' }],
            },
            size: {
                type: 'boolean',
                title: '',
                id: 'size',
                param: '',
                values: [{ value: '1', found: 1, id: '1', checked: true }],
            },
        };
        const { appliedFilters, disabledFilters } = getAppliedAndDisableFilters(defaultOrder, filters);

        expect(appliedFilters.size).toBeDefined();
        expect(disabledFilters.brand).toBeDefined();
    });

    it('Возвращает примененные number фильтры', () => {
        const defaultOrder = ['brand', 'shop', 'size'];
        const filters: IFilters = {
            brand: {
                type: 'number',
                title: '',
                id: 'brand',
                param: '',
                value: { min: 1, max: 10 },
                min: 4,
                max: 8,
            },
            shop: {
                type: 'number',
                title: '',
                id: 'shop',
                param: '',
                value: { min: 1, max: 10 },
            },
            size: {
                type: 'number',
                title: '',
                id: 'size',
                param: '',
                value: { min: 1, max: 10 },
            },
        };
        const { appliedFilters, disabledFilters } = getAppliedAndDisableFilters(defaultOrder, filters);

        expect(appliedFilters.brand).toBeDefined();
        expect(disabledFilters).toStrictEqual({});
    });
});
