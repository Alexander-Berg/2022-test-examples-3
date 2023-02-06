jest.disableAutomock();

import {
    ALL_TYPE,
    BUS_TYPE,
    TRAIN_TYPE,
    PLANE_TYPE,
    SUBURBAN_TYPE,
} from '../../transportType';

import transport from '../transport';

describe('transport filter', () => {
    describe('isDefaultValue', () => {
        it('Вернёт true для пустого массива', () => {
            expect(transport.isDefaultValue([])).toBe(true);
        });

        it('Вернёт false для непустого массива', () => {
            expect(transport.isDefaultValue([BUS_TYPE])).toBe(false);
        });
    });

    describe('apply', () => {
        const value = [TRAIN_TYPE, PLANE_TYPE, BUS_TYPE];

        it('Вернёт true если тип транспорта содержится в заданном значении', () => {
            const segment = {
                transport: {
                    code: PLANE_TYPE,
                },
            };

            expect(transport.apply(value, segment)).toBe(true);
        });

        it('Вернёт false если тип транспорта не содержится в заданном значении', () => {
            const segment = {
                transport: {
                    code: SUBURBAN_TYPE,
                },
            };

            expect(transport.apply(value, segment)).toBe(false);
        });

        it(`Вернёт true для сегмента с пересадками, если все вложенные сегменты
            соответствует заданному значению фильтра`, () => {
            const segment = {
                isTransfer: true,
                segments: [
                    {
                        transport: {
                            code: TRAIN_TYPE,
                        },
                    },
                    {
                        transport: {
                            code: PLANE_TYPE,
                        },
                    },
                ],
            };

            expect(transport.apply(value, segment)).toBe(true);
        });

        it(`Вернёт false для сегмента с пересадками, если хотя бы один вложенный сегмент
            не соответствует заданному значению фильтра`, () => {
            const segment = {
                isTransfer: true,
                segments: [
                    {
                        transport: {
                            code: SUBURBAN_TYPE,
                        },
                    },
                    {
                        transport: {
                            code: TRAIN_TYPE,
                        },
                    },
                ],
            };

            expect(transport.apply(value, segment)).toBe(false);
        });
    });

    describe('updateOptions', () => {
        it('Если тип транспорта присутствует в опциях - опции фильтра вернутся без изменений', () => {
            const options = [BUS_TYPE, PLANE_TYPE, TRAIN_TYPE];
            const segment = {
                transport: {
                    code: PLANE_TYPE,
                },
            };
            const result = transport.updateOptions(options, segment);

            expect(result).toEqual(expect.arrayContaining(options));
        });

        it('Если тип транспорта отсутствует в опциях - вернёт опции с добавленным типом транспорта', () => {
            const options = [PLANE_TYPE, TRAIN_TYPE];
            const segment = {
                transport: {
                    code: BUS_TYPE,
                },
            };
            const result = transport.updateOptions(options, segment);

            expect(result).toEqual(
                expect.arrayContaining([PLANE_TYPE, TRAIN_TYPE, BUS_TYPE]),
            );
        });

        it('Расширит опции всеми видами транспорта содержащимися в пересадочном сегменте', () => {
            const options = [];
            const segment = {
                isTransfer: true,
                segments: [
                    {
                        transport: {
                            code: PLANE_TYPE,
                        },
                    },
                    {
                        transport: {
                            code: TRAIN_TYPE,
                        },
                    },
                ],
            };
            const result = transport.updateOptions(options, segment);

            expect(result).toEqual(
                expect.arrayContaining([TRAIN_TYPE, PLANE_TYPE]),
            );
        });
    });

    describe('serializeToQuery', () => {
        it('Вернёт объект с полем transport и заданными значениями', () => {
            const value = [TRAIN_TYPE, PLANE_TYPE, BUS_TYPE];

            expect(transport.serializeToQuery(value)).toEqual({
                transport: value,
            });
        });
    });

    describe('deserializeFromQuery', () => {
        it('Для пустого объекта вернёт дефолтное значение', () => {
            expect(transport.deserializeFromQuery({})).toEqual(
                transport.getDefaultValue(),
            );
        });

        it('Вернёт опции с единственным значением', () => {
            const query = {
                transport: BUS_TYPE,
            };

            expect(transport.deserializeFromQuery(query)).toEqual([BUS_TYPE]);
        });

        it('Вернёт отсортированный список значени (сортировка по FILTERABLE_TRANSPORT_TYPES)', () => {
            const query = {
                transport: [BUS_TYPE, PLANE_TYPE],
            };

            expect(transport.deserializeFromQuery(query)).toEqual([
                PLANE_TYPE,
                BUS_TYPE,
            ]);
        });

        it('Отфильтрует невалидные значения', () => {
            const query = {
                transport: [BUS_TYPE, 'spaceship'],
            };

            expect(transport.deserializeFromQuery(query)).toEqual([BUS_TYPE]);
        });

        it('Удалит дубликаты', () => {
            const query = {
                transport: [BUS_TYPE, TRAIN_TYPE, BUS_TYPE],
            };

            expect(transport.deserializeFromQuery(query)).toEqual([
                TRAIN_TYPE,
                BUS_TYPE,
            ]);
        });
    });

    describe('isAvailableForContext', () => {
        it('Вернеёт true для поиска всеми видами транспорта', () => {
            const context = {
                transportType: ALL_TYPE,
            };

            expect(transport.isAvailableForContext(context)).toBe(true);
        });

        it('Вернёт false для поисков конкретным типом транспорта', () => {
            const context = {
                transportType: BUS_TYPE,
            };

            expect(transport.isAvailableForContext(context)).toBe(false);
        });
    });

    describe('isAvailableWithOptions', () => {
        it('Вернёт true если в поиске более одного типа транспорта', () => {
            const options = [PLANE_TYPE, TRAIN_TYPE];

            expect(transport.isAvailableWithOptions(options)).toBe(true);
        });

        it('Вернёт false если в поиске только один тип транспорта', () => {
            const options = [PLANE_TYPE];

            expect(transport.isAvailableWithOptions(options)).toBe(false);
        });
    });

    describe('validateValue', () => {
        const options = [BUS_TYPE, TRAIN_TYPE];

        it('Вернёт тот же список, если значение соответствует опциям фильтра', () => {
            const value = [BUS_TYPE];

            expect(transport.validateValue(value, options)).toEqual(value);
        });

        it('Вернёт только те типы транспорта, которые содержатся в опциях фильтра', () => {
            const value = [BUS_TYPE, SUBURBAN_TYPE, PLANE_TYPE];

            expect(transport.validateValue(value, options)).toEqual([BUS_TYPE]);
        });

        it('Вернёт дефолтное значение если типы транспорта не содержатся в опциях фильтра', () => {
            const value = [SUBURBAN_TYPE, PLANE_TYPE];

            expect(transport.validateValue(value, options)).toEqual(
                transport.getDefaultValue(),
            );
        });
    });
});
