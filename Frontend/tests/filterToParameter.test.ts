import type { IRawAppliedNumberFilter, IRawAppliedBooleanFilter, IRawAppliedEnumFilter } from '../types';
import { filterToParameter } from '../filterToParameter';

describe('filterToParameter', () => {
    it('Должен преобразовывать фильтр с типом number', () => {
        const filter: IRawAppliedNumberFilter = {
            type: 'number',
            name: 'Вес',
            unit: 'кг',
            values: [{
                max: '1.8',
                min: '1.8',
            }],
        };
        const result = filterToParameter(filter);
        expect(result?.[0]).toBe('Вес');
        expect(result?.[1]).toBe('1.8кг');
    });

    it('Должен преобразовывать фильтр с типом number без unit', () => {
        const filter: IRawAppliedNumberFilter = {
            type: 'number',
            name: 'Вес',
            values: [{
                max: '1.8',
                min: '1.8',
            }],
        };
        const result = filterToParameter(filter);
        expect(result?.[0]).toBe('Вес');
        expect(result?.[1]).toBe('1.8');
    });

    it('Должен преобразовывать фильтр с типом boolean', () => {
        const filter: IRawAppliedBooleanFilter = {
            type: 'boolean',
            name: 'Чемодан/кейс в комплекте',
            values: [
                { found: 0, value: '1' },
                { found: 1, value: '0' },
            ],
        };
        const result = filterToParameter(filter);
        expect(result?.[0]).toBe('Чемодан/кейс в комплекте');
        expect(result?.[1]).toBe('Нет');
    });

    describe('Должен преобразовывать фильтр с типом enum', () => {
        it('с единственным значением', () => {
            const filter: IRawAppliedEnumFilter = {
                type: 'enum',
                name: 'Тип',
                values: [{
                    value: 'кромочный (триммер)',
                }],
            };
            const result = filterToParameter(filter);
            expect(result?.[0]).toBe('Тип');
            expect(result?.[1]).toBe('кромочный (триммер)');
        });

        it('с несколькими значениями', () => {
            const filter: IRawAppliedEnumFilter = {
                type: 'enum',
                name: 'Тип',
                values: [{
                    value: 'кромочный (триммер)',
                }, {
                    value: 'узорчатый',
                }],
            };
            const result = filterToParameter(filter);
            expect(result?.[0]).toBe('Тип');
            expect(result?.[1]).toBe('кромочный (триммер), узорчатый');
        });
    });

    it('Должен вернуть undefined для неизвестного типа фильтра', () => {
        const filter = {
            type: 'qwe',
        };
        //@ts-expect-error
        const result = filterToParameter(filter);
        expect(result).toBeUndefined();
    });
});
