import { normalize } from 'normalizr';
import { filterSchema } from '../schema';
import { RADIO_NO_MATTER_ID, RADIO_NO_MATTER_FOUND } from '../constants';
import { IRangeFilter, IBooleanFilter } from '../types';
import { rangeFilter, rangeFilterWithSelectedVal } from './data/rangeFilter';
import { booleanFilter, booleanFilterWithSelectedVal } from './data/booleanFilter';
import { radioFilter, radioFilterWithSelectedVal } from './data/radioFilter';
import { enumFilter, enumFilterWithSelectedVal, enumFilterColor } from './data/enumFiilter';

describe('filters/schema', () => {
    describe('фильтр Range', () => {
        const minValId = `${rangeFilter.id}_min`;
        const maxValId = `${rangeFilter.id}_max`;

        it('корректно нормальзуются постоянные значения', () => {
            // @ts-ignore
            const filter = normalize(rangeFilter, filterSchema).entities.filter[rangeFilter.id];

            expect(filter).toMatchObject({
                id: rangeFilter.id,
                type: 'range',
                subType: rangeFilter.subType,
                initialType: rangeFilter.type,
                name: `${rangeFilter.name}`,
                unit: '',
                maxConstraint: rangeFilter.values[0].max,
                minConstraint: rangeFilter.values[0].min,
            });
        });

        it('корректно нормализует заголовок с учетом unit', () => {
            // @ts-ignore
            const filter = normalize({
                ...rangeFilter,
                unit: 'USD',
            }, filterSchema).entities.filter[rangeFilter.id];

            expect(filter.name).toEqual(`${rangeFilter.name}, USD`);
        });

        it('корректно нормализует заголовок для фильтра "Цена"', () => {
            // @ts-ignore
            const filter = normalize({
                ...rangeFilter,
                id: 'glprice',
            }, filterSchema).entities.filter.glprice;

            expect(filter.name).toEqual(`${rangeFilter.name}, ₽`);
        });

        it('корректно нормализуются значения фильтра', () => {
            // @ts-ignore
            const filter = normalize(rangeFilter, filterSchema).entities.filter[rangeFilter.id];

            expect(filter).toMatchObject({
                valuesIds: [minValId, maxValId],
                valuesMap: {
                    [minValId]: {
                        id: minValId,
                        value: '',
                    },
                    [maxValId]: {
                        id: maxValId,
                        value: '',
                    },
                },
            });
        });

        it('корректно должны проставляться выбранные значения фильтра', () => {
            // @ts-ignore
            const filter = <IRangeFilter>normalize(rangeFilterWithSelectedVal, filterSchema).entities.filter[rangeFilterWithSelectedVal.id];

            expect(filter).toMatchObject({
                valuesMap: {
                    [minValId]: expect.objectContaining({ value: rangeFilterWithSelectedVal.values[0].min }),
                    [maxValId]: expect.objectContaining({ value: rangeFilterWithSelectedVal.values[0].max }),
                },
            });
        });
    });
    describe('фильтр Boolean', () => {
        it('корректно нормализуется по умолчанию(без выбранных значений)', () => {
            // @ts-ignore
            const filter = normalize(booleanFilter, filterSchema).entities.filter[booleanFilter.id];
            const valId1 = `${booleanFilter.id}_${booleanFilter.values[0].value}`;
            const valId2 = `${booleanFilter.id}_${booleanFilter.values[1].value}`;

            expect(filter).toEqual({
                id: booleanFilter.id,
                type: 'boolean',
                subType: booleanFilter.subType,
                initialType: booleanFilter.type,
                name: booleanFilter.name,
                found: {
                    [valId1]: booleanFilter.values[0].found,
                    [valId2]: booleanFilter.values[1].found,
                },
                valuesIds: [valId1, valId2],
                selectedValuesIds: [valId2],
                valuesMap: {
                    [valId1]: { id: valId1, value: booleanFilter.values[0].value, checked: true },
                    [valId2]: { id: valId2, value: booleanFilter.values[1].value, checked: false },
                },
            });
        });

        it('если в ответе есть выбранные значения, то они должны корректно проставляться', () => {
            // @ts-ignore
            const filter = <IBooleanFilter>normalize(booleanFilterWithSelectedVal, filterSchema).entities.filter[booleanFilterWithSelectedVal.id];
            const valId1 = `${booleanFilterWithSelectedVal.id}_${booleanFilterWithSelectedVal.values[0].value}`;

            expect(filter.selectedValuesIds).toEqual([valId1]);
        });
    });
    describe('Фильтр Radio', () => {
        it('корректно нормализуется по умолчанию(без выбранных значений)', () => {
            // @ts-ignore
            const filter = normalize(radioFilter, filterSchema).entities.filter[radioFilter.id];
            const valId1 = `${radioFilter.id}_${radioFilter.values[0].value}`;
            const valId2 = `${radioFilter.id}_${radioFilter.values[1].value}`;
            const noMatterId = `${radioFilter.id}_${RADIO_NO_MATTER_ID}`;

            expect(filter).toEqual({
                id: radioFilter.id,
                name: radioFilter.name,
                type: 'radio',
                initialType: radioFilter.type,
                subType: radioFilter.subType,
                found: {
                    [valId1]: radioFilter.values[0].found,
                    [valId2]: radioFilter.values[1].found,
                    [noMatterId]: RADIO_NO_MATTER_FOUND,
                },
                valuesIds: [valId1, valId2, noMatterId],
                selectedValuesIds: [noMatterId],
                valuesMap: {
                    [valId1]: { id: valId1, value: radioFilter.values[0].value, checked: false },
                    [valId2]: { id: valId2, value: radioFilter.values[1].value, checked: false },
                    [noMatterId]: { id: noMatterId, value: 'no_matter', checked: true },
                },
            });
        });

        it('если в ответе есть выбранные значения, то они должны корректно проставляться', () => {
            // @ts-ignore
            const filter = normalize(radioFilterWithSelectedVal, filterSchema).entities.filter[radioFilterWithSelectedVal.id];
            const valId1 = `${radioFilterWithSelectedVal.id}_${radioFilterWithSelectedVal.values[0].value}`;
            const valId2 = `${radioFilterWithSelectedVal.id}_${radioFilterWithSelectedVal.values[1].value}`;
            const noMatterId = `${radioFilter.id}_${RADIO_NO_MATTER_ID}`;

            expect(filter).toMatchObject({
                selectedValuesIds: [valId2],
                valuesMap: {
                    [valId1]: expect.objectContaining({ checked: false }),
                    [valId2]: expect.objectContaining({ checked: true }),
                    [noMatterId]: expect.objectContaining({ checked: false }),
                },
            });
        });
    });
    describe('Фильтр Enum', () => {
        it('корректно нормализуется по умолчанию(без выбранных значений)', () => {
            // @ts-ignore
            const filter = normalize(enumFilter, filterSchema).entities.filter[enumFilter.id];
            const valId1 = enumFilter.values[0].id;
            const valId2 = enumFilter.values[1].id;
            const valId3 = enumFilter.values[2].id;

            expect(filter).toEqual({
                id: enumFilter.id,
                type: 'enum',
                name: enumFilter.name,
                initialType: enumFilter.type,
                subType: enumFilter.subType,
                selectedValuesIds: [],
                found: {
                    [valId1]: enumFilter.values[0].found,
                    [valId2]: enumFilter.values[1].found,
                    [valId3]: enumFilter.values[2].found,
                },
                valuesIds: [valId1, valId2, valId3],
                valuesMap: {
                    [valId1]: { id: valId1, value: enumFilter.values[0].value },
                    [valId2]: { id: valId2, value: enumFilter.values[1].value },
                    [valId3]: { id: valId3, value: enumFilter.values[2].value },
                },
                valuesGroups: {
                    top: [valId2],
                    all: [valId1, valId2, valId3],
                },
            });
        });

        it('если в ответе есть выбранные значения, то они должны корректно проставляться', () => {
            // @ts-ignore
            const filter = normalize(enumFilterWithSelectedVal, filterSchema).entities.filter[enumFilterWithSelectedVal.id];
            const valId1 = enumFilterWithSelectedVal.values[0].id;
            const valId2 = enumFilterWithSelectedVal.values[1].id;
            const valId3 = enumFilterWithSelectedVal.values[2].id;

            expect(filter).toMatchObject({
                selectedValuesIds: [valId1, valId2],
                valuesMap: {
                    [valId1]: expect.objectContaining({ checked: true }),
                    [valId2]: expect.objectContaining({ checked: true }),
                    [valId3]: expect.objectContaining({ checked: undefined }),
                },
            });
        });

        it('если значением фильтра является цвет, то должна проставляться опция code', () => {
            // @ts-ignore
            const filter = normalize(enumFilterColor, filterSchema).entities.filter[enumFilterColor.id];
            const val1 = enumFilterColor.values[0];

            expect(filter).toMatchObject({
                valuesMap: {
                    [val1.id]: expect.objectContaining({ code: val1.code }),
                },
            });
        });
    });

    describe('фильтр Unknown', () => {
        it('корректно нормализуется не поддерживаемый фильтр', () => {
            const data = { id: '8988', type: 'super-filter' };
            // @ts-ignore
            const filter = normalize(data, filterSchema).entities.filter[data.id];

            expect(filter).toEqual({
                id: '__unknown__',
                type: '__unknown__',
                name: '__unknown__',
                initialType: data.type,
                subType: '',
                valuesIds: [],
            });
        });
    });
});
