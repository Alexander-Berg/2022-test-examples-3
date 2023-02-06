import cloneDeep from 'lodash/cloneDeep';
import type {
    IFilters,
    IEnumFilter,
    IBooleanFilter,
    INumberFilter,
} from '@src/typings/filters';
import {
    getFiltersParams,
    equalFiltersParams,
    getFiltersActionPayloadFromParams,
} from '../utils';

describe('SearchUtils', () => {
    describe('getFiltersParams()', () => {
        it('Возвращает параметры всех типов фильтров', () => {
            const filters: IFilters = {
                '4925670': {
                    values: [
                        {
                            value: 'Android',
                            found: 50,
                            id: '12105164',
                            checked: true,
                        },
                        {
                            value: 'Android без сервисов Google',
                            found: 3,
                            id: '34891650',
                        },
                        {
                            value: 'iOS',
                            found: 0,
                            id: '12105169',
                        },
                    ],
                    title: 'Операционная система',
                    type: 'enum',
                    param: 'glfilter',
                    id: '4925670',
                },
                '4925675': {
                    values: [
                        {
                            value: '1',
                            found: 40,
                            id: '1',
                            checked: true,
                        },
                        {
                            value: '0',
                            found: 10,
                            id: '0',
                        },
                    ],
                    title: 'Слот для карты памяти',
                    type: 'boolean',
                    param: 'glfilter',
                    id: '4925675',
                },
                '7265588': {
                    id: '7265588',
                    title: 'Разрешение фронтальной камеры',
                    type: 'number',
                    param: 'glfilter',
                    min: 5,
                    max: 32,
                    withSlider: false,
                    value: {
                        max: 30,
                    },
                },
                '7853110': {
                    values: [
                        {
                            value: '1',
                            found: 0,
                            id: '12105592',
                        },
                        {
                            value: '2',
                            found: 50,
                            id: '12105593',
                        },
                    ],
                    title: 'Количество SIM-карт',
                    type: 'enum',
                    param: 'glfilter',
                    id: '7853110',
                },
                '7893318': {
                    values: [
                        {
                            id: '459710',
                            found: 6,
                            value: 'HUAWEI',
                            checked: true,
                        },
                        {
                            id: '153061',
                            found: 44,
                            value: 'Samsung',
                            checked: true,
                        },
                        {
                            id: '153043',
                            found: 0,
                            value: 'Apple',
                        },
                        {
                            id: '152863',
                            found: 1,
                            value: 'ASUS',
                        },
                        {
                            id: '28660591',
                            found: 2,
                            value: 'AYYA',
                        },
                    ],
                    title: 'Производитель',
                    type: 'enum',
                    param: 'glfilter',
                    id: '7893318',
                },
                '14805766': {
                    id: '14805766',
                    title: 'Диагональ экрана (точно)',
                    type: 'number',
                    param: 'glfilter',
                    min: 6,
                    max: 6,
                    withSlider: false,
                    value: {},
                },
                '15164206': {
                    values: [
                        {
                            value: 'до 5 Мпикс',
                            found: 0,
                            id: '15164214',
                        },
                        {
                            value: '5-9 Мпикс',
                            found: 2,
                            id: '15164211',
                        },
                        {
                            value: '10-13 Мпикс',
                            found: 4,
                            id: '15164213',
                        },
                        {
                            value: '13.1 Мпикс и выше',
                            found: 44,
                            id: '15164212',
                        },
                    ],
                    title: 'Разрешение основной камеры',
                    type: 'enum',
                    param: 'glfilter',
                    id: '15164206',
                },
                '17697956': {
                    values: [
                        {
                            value: '60 Гц',
                            found: 4,
                            id: '17697968',
                        },
                        {
                            value: '90 Гц',
                            found: 26,
                            id: '17697961',
                        },
                        {
                            value: '120 Гц',
                            found: 10,
                            id: '17697963',
                        },
                    ],
                    title: 'Частота обновления экрана',
                    type: 'enum',
                    param: 'glfilter',
                    id: '17697956',
                },
                '22371211': {
                    values: [
                        {
                            value: '1',
                            found: 4,
                            id: '1',
                        },
                        {
                            value: '0',
                            found: 46,
                            id: '0',
                        },
                    ],
                    title: 'Гибкий экран',
                    type: 'boolean',
                    param: 'glfilter',
                    id: '22371211',
                },
                '27563490': {
                    values: [
                        {
                            value: '2K QHD',
                            found: 0,
                            id: '27563510',
                        },
                        {
                            value: 'Full HD',
                            found: 30,
                            id: '27563590',
                        },
                        {
                            value: 'HD',
                            found: 20,
                            id: '27563610',
                        },
                        {
                            value: 'SD',
                            found: 0,
                            id: '27563630',
                        },
                    ],
                    title: 'Формат разрешения экрана',
                    type: 'enum',
                    param: 'glfilter',
                    id: '27563490',
                },
                '29641890': {
                    values: [
                        {
                            value: 'идеальное',
                            found: 0,
                            id: '29641910',
                        },
                    ],
                    title: 'Состояние товара',
                    type: 'enum',
                    param: 'glfilter',
                    id: '29641890',
                },
                fesh: {
                    values: [
                        {
                            id: '4852513',
                            found: 83,
                            value: 'Clevercel',
                            checked: true,
                        },
                        {
                            id: '3850317',
                            found: 258,
                            value: 'navitoys.ru',
                        },
                        {
                            id: '6595330',
                            found: 77,
                            value: 'appstore-mos.ru',
                        },
                        {
                            id: '905379',
                            found: 58,
                            value: 'GB Store',
                            checked: true,
                        },
                        {
                            id: '6640143',
                            found: 85,
                            value: 'glasscase63.ru',
                            checked: true,
                        },
                    ],
                    title: 'Магазины',
                    type: 'enum',
                    param: 'fesh',
                    id: 'fesh',
                },
                glprice: {
                    id: 'glprice',
                    title: 'Цена',
                    type: 'number',
                    param: 'glprice',
                    min: 8870,
                    max: 78990,
                    withSlider: true,
                    value: {
                        min: 20000,
                        max: 80000,
                    },
                },
            };

            expect(getFiltersParams(filters)).toEqual({
                glfilter: ['4925670:12105164', '4925675:1', '7265588:~30', '7893318:459710,153061'],
                fesh: '4852513,905379,6640143',
                glprice: '20000~80000',
            });
        });

        it('Возвращает параметры только для цены «от»', () => {
            const filters: IFilters = {
                glprice: {
                    id: 'glprice',
                    title: 'Цена',
                    type: 'number',
                    param: 'glprice',
                    min: 8870,
                    max: 78990,
                    withSlider: true,
                    value: {
                        min: 20000,
                    },
                },
            };

            expect(getFiltersParams(filters)).toEqual({
                glprice: '20000~',
            });
        });
    });

    describe('equalFiltersParams()', () => {
        it('Сравнивает один ключ при одинаковом порядке фильтров', () => {
            expect(equalFiltersParams(
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:~30', '7893318:459710,153061'],
                    glprice: '~80000',
                },
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:~30', '7893318:459710,153061'],
                    priceto: '80000',
                    order: 'dpop',
                    text: 'iphone',
                },
                ['glfilter'],
            )).toBe(true);
        });

        it('Сравнивает несколько ключей при одинаковом порядке фильтров', () => {
            expect(equalFiltersParams(
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:~30', '7893318:459710,153061'],
                    fesh: '4852513,3850317',
                },
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:~30', '7893318:459710,153061'],
                    fesh: '4852513,3850317',
                    text: 'iphone',
                },
                ['glfilter', 'fesh'],
            )).toBe(true);
        });

        it('Сообщает об отличии при отсутствии ключа', () => {
            expect(equalFiltersParams(
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:~30', '4521245:459710'],
                    glprice: '~80000',
                },
                {
                    priceto: '80000',
                    order: 'dpop',
                    text: 'iphone',
                },
                ['glfilter'],
            )).toBe(false);
        });

        it('Сообщает об отличии при несоответствии типа ключа', () => {
            expect(equalFiltersParams(
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:~30', '4521245:459710'],
                    glprice: '~80000',
                },
                {
                    glfilter: '4925670:12105164',
                    priceto: '80000',
                    order: 'dpop',
                    text: 'iphone',
                },
                ['glfilter'],
            )).toBe(false);
        });

        it('Сообщает об отличии при наличии разных фильтров', () => {
            expect(equalFiltersParams(
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:~30', '4521245:459710'],
                    glprice: '~80000',
                },
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:~30', '7893318:459710,153061'],
                    priceto: '80000',
                    order: 'dpop',
                    text: 'iphone',
                },
                ['glfilter'],
            )).toBe(false);
        });

        it('Определяет одинаковый набор фильтров при разном порядке фильтров', () => {
            expect(equalFiltersParams(
                {
                    glfilter: ['4925670:12105164', '7893318:459710,153061', '4925675:1', '7265588:20~30'],
                    glprice: '~80000',
                },
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:20~30', '7893318:459710,153061'],
                    priceto: '80000',
                    order: 'dpop',
                    text: 'iphone',
                },
                ['glfilter'],
            )).toBe(true);
        });

        it('Определяет одинаковый набор фильтров при разном порядке значений фильтров', () => {
            expect(equalFiltersParams(
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:20~', '7893318:153061,459710,100500'],
                    glprice: '~80000',
                },
                {
                    glfilter: ['4925670:12105164', '4925675:1', '7265588:20~', '7893318:100500,153061,459710'],
                    priceto: '80000',
                    order: 'dpop',
                    text: 'iphone',
                },
                ['glfilter'],
            )).toBe(true);
        });
    });

    describe('getFiltersActionPayloadFromParams()', () => {
        let filters: IFilters;

        beforeEach(() => {
            filters = {
                '4925670': {
                    values: [
                        {
                            value: 'Android',
                            found: 50,
                            id: '12105164',
                        },
                        {
                            value: 'Android без сервисов Google',
                            found: 3,
                            id: '34891650',
                        },
                        {
                            value: 'iOS',
                            found: 0,
                            id: '12105169',
                        },
                    ],
                    title: 'Операционная система',
                    type: 'enum',
                    param: 'glfilter',
                    id: '4925670',
                },
                '4925675': {
                    values: [
                        {
                            value: '1',
                            found: 40,
                            id: '1',
                        },
                        {
                            value: '0',
                            found: 10,
                            id: '0',
                        },
                    ],
                    title: 'Слот для карты памяти',
                    type: 'boolean',
                    param: 'glfilter',
                    id: '4925675',
                },
                '7265588': {
                    id: '7265588',
                    title: 'Разрешение фронтальной камеры',
                    type: 'number',
                    param: 'glfilter',
                    min: 5,
                    max: 32,
                    withSlider: false,
                    value: {},
                },
                '7853110': {
                    values: [
                        {
                            value: '1',
                            found: 0,
                            id: '12105592',
                        },
                        {
                            value: '2',
                            found: 50,
                            id: '12105593',
                        },
                    ],
                    title: 'Количество SIM-карт',
                    type: 'enum',
                    param: 'glfilter',
                    id: '7853110',
                },
                '7893318': {
                    values: [
                        {
                            id: '459710',
                            found: 6,
                            value: 'HUAWEI',
                        },
                        {
                            id: '153061',
                            found: 44,
                            value: 'Samsung',
                        },
                        {
                            id: '153043',
                            found: 0,
                            value: 'Apple',
                        },
                    ],
                    title: 'Производитель',
                    type: 'enum',
                    param: 'glfilter',
                    id: '7893318',
                },
                '14805766': {
                    id: '14805766',
                    title: 'Диагональ экрана (точно)',
                    type: 'number',
                    param: 'glfilter',
                    min: 6,
                    max: 6,
                    withSlider: false,
                    value: {},
                },
                '22371211': {
                    values: [
                        {
                            value: '1',
                            found: 4,
                            id: '1',
                        },
                        {
                            value: '0',
                            found: 46,
                            id: '0',
                        },
                    ],
                    title: 'Гибкий экран',
                    type: 'boolean',
                    param: 'glfilter',
                    id: '22371211',
                },
                fesh: {
                    values: [
                        {
                            id: '4852513',
                            found: 83,
                            value: 'Clevercel',
                            checked: true,
                        },
                        {
                            id: '3850317',
                            found: 258,
                            value: 'navitoys.ru',
                        },
                        {
                            id: '6595330',
                            found: 77,
                            value: 'appstore-mos.ru',
                        },
                        {
                            id: '905379',
                            found: 58,
                            value: 'GB Store',
                            checked: true,
                        },
                    ],
                    title: 'Магазины',
                    type: 'enum',
                    param: 'fesh',
                    id: 'fesh',
                },
                glprice: {
                    id: 'glprice',
                    title: 'Цена',
                    type: 'number',
                    param: 'glprice',
                    min: 8870,
                    max: 78990,
                    withSlider: true,
                    value: {},
                },
            };
        });

        it('Сбрасывает все указанные фильтры при отсутствии параметров', () => {
            const checkedFilters = cloneDeep(filters);

            // Значения, которые должны сброситься.
            (checkedFilters[4925670] as IEnumFilter).values[1].checked = true;
            (checkedFilters[4925675] as IBooleanFilter).values[0].checked = true;
            (checkedFilters[7265588] as INumberFilter).value.min = 10;

            // Значения, которые должны остаться.
            (checkedFilters.fesh as IEnumFilter).values[0].checked = true;
            (filters.fesh as IEnumFilter).values[0].checked = true;

            expect(getFiltersActionPayloadFromParams(checkedFilters as IFilters, {}, ['glfilter'])).toEqual(filters);
        });

        it('Устанавливает значение одного фильтра из glfilter', () => {
            const checkedFilters = cloneDeep(filters);

            // Значения, которые должны установиться.
            (checkedFilters[4925670] as IEnumFilter).values[1].checked = true;

            const params = { glfilter: '4925670:34891650' };
            expect(getFiltersActionPayloadFromParams(filters, params, ['glfilter'])).toEqual(checkedFilters);
        });

        it('Сбрасывает все старые значения параметра и устанавливает новые', () => {
            const checkedFilters = cloneDeep(filters);

            // Значения, которые должны сброситься.
            (filters[4925675] as IBooleanFilter).values[0].checked = true;
            (filters[7265588] as INumberFilter).value.min = 10;

            // Значения, которые должны установиться.
            (checkedFilters[4925670] as IEnumFilter).values[1].checked = true;

            const params = { glfilter: '4925670:34891650' };
            expect(getFiltersActionPayloadFromParams(filters, params, ['glfilter'])).toEqual(checkedFilters);
        });

        it('Устанавливает значения всем типам фильтров', () => {
            const checkedFilters = cloneDeep(filters);

            // Значения, которые должны установиться.
            (checkedFilters[4925670] as IEnumFilter).values[1].checked = true;
            (checkedFilters[4925675] as IBooleanFilter).values[0].checked = true;
            (checkedFilters[7265588] as INumberFilter).value.max = 30;
            (checkedFilters[7893318] as IEnumFilter).values[0].checked = true;
            (checkedFilters[7893318] as IEnumFilter).values[2].checked = true;
            (checkedFilters.fesh as IEnumFilter).values[0].checked = true;
            (checkedFilters.fesh as IEnumFilter).values[2].checked = true;
            (checkedFilters.fesh as IEnumFilter).values[3].checked = true;

            // Значения, которые должны остаться.
            (filters.glprice as INumberFilter).value.max = 80000;
            (checkedFilters.glprice as INumberFilter).value.max = 80000;

            // Значения, которые должны сброситься.
            (filters[7265588] as INumberFilter).value.min = 10;

            const params = {
                glfilter: ['4925670:34891650', '4925675:1', '7265588:~30', '7893318:459710,153043'],
                fesh: '4852513,6595330,905379',
                priceto: '80000',
                order: 'dpop',
                text: 'iphone',
            };
            expect(getFiltersActionPayloadFromParams(filters, params, ['glfilter', 'fesh'])).toEqual(checkedFilters);
        });
    });
});
