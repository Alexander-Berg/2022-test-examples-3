import MockDate from 'mockdate';

import {AVIA_CLASSES} from 'constants/avia';

import {EOneWay} from 'server/services/AviaSearchService/types/IAviaParams';

import {sanitizeAviaSearchParams} from 'projects/avia/lib/search/sanitizeSearchParams';

describe('sanitizeAviaSearchParams', () => {
    const testMockDate = '2020-02-14';

    beforeEach(() => {
        MockDate.set(testMockDate);
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('нет параметров - вернёт пустой объект', () => {
        expect(sanitizeAviaSearchParams({})).toEqual({});
    });

    describe('when', () => {
        it('невалидная дата', () => {
            expect(
                sanitizeAviaSearchParams({
                    when: 'maybe tomorrow',
                }),
            ).toEqual({});
        });

        it('дата в прошлом', () => {
            expect(
                sanitizeAviaSearchParams({
                    when: '2020-02-13',
                }),
            ).toEqual({});
        });

        it('валидная дата', () => {
            expect(
                sanitizeAviaSearchParams({
                    when: '2020-02-14',
                }),
            ).toEqual({
                when: '2020-02-14',
            });
        });
    });

    describe('return_date', () => {
        it('незаполненная дата', () => {
            expect(
                sanitizeAviaSearchParams({
                    return_date: '',
                }),
            ).toEqual({
                return_date: '',
            });
        });

        it('невалидная дата', () => {
            expect(
                sanitizeAviaSearchParams({
                    return_date: '01-03-2020',
                }),
            ).toEqual({});
        });

        it('дата в прошлом', () => {
            expect(
                sanitizeAviaSearchParams({
                    return_date: '2020-02-01',
                }),
            ).toEqual({});
        });

        it('валидная дата', () => {
            expect(
                sanitizeAviaSearchParams({
                    return_date: '2020-03-01',
                }),
            ).toEqual({
                return_date: '2020-03-01',
            });
        });
    });

    describe('seats', () => {
        it('seats не число', () => {
            expect(
                sanitizeAviaSearchParams({
                    adult_seats: 'III',
                    children_seats: 'two',
                    infant_seats: 'Pi',
                }),
            ).toEqual({});
        });

        it('должен быть как минимум один взрослый', () => {
            expect(
                sanitizeAviaSearchParams({
                    adult_seats: '0',
                }),
            ).toEqual({});
        });

        it('заполнено только одно поле', () => {
            expect(
                sanitizeAviaSearchParams({
                    adult_seats: '2',
                }),
            ).toEqual({
                adult_seats: '2',
            });
        });

        it('дефолтные значения', () => {
            expect(
                sanitizeAviaSearchParams({
                    adult_seats: '1',
                    children_seats: '0',
                    infant_seats: '0',
                }),
            ).toEqual({
                adult_seats: '1',
                children_seats: '0',
                infant_seats: '0',
            });
        });

        it('seats > 9', () => {
            expect(
                sanitizeAviaSearchParams({
                    adult_seats: '10',
                    children_seats: '11',
                    infant_seats: '12',
                }),
            ).toEqual({});
        });

        it('в сумме количество пассажиров > 9', () => {
            expect(
                sanitizeAviaSearchParams({
                    adult_seats: '3',
                    children_seats: '3',
                    infant_seats: '4',
                }),
            ).toEqual({});
        });

        it('валидное кол-во пассажиров', () => {
            expect(
                sanitizeAviaSearchParams({
                    adult_seats: '4',
                    children_seats: '3',
                    infant_seats: '2',
                }),
            ).toEqual({
                adult_seats: '4',
                children_seats: '3',
                infant_seats: '2',
            });
        });
    });

    describe('klass', () => {
        it('содержит невалидное значение', () => {
            expect(
                sanitizeAviaSearchParams({
                    klass: 'bussines' as any,
                }),
            ).toEqual({});
        });

        AVIA_CLASSES.forEach(klass => {
            it(`содержит валидное значение ${klass}`, () => {
                expect(
                    sanitizeAviaSearchParams({
                        klass,
                    }),
                ).toEqual({
                    klass,
                });
            });
        });
    });

    describe('oneway', () => {
        const validOnewayValues: EOneWay[] = [
            EOneWay.ONE_WAY,
            EOneWay.ROUND_TRIP,
        ];

        it('содержит невалидное значение', () => {
            expect(
                sanitizeAviaSearchParams({
                    // @ts-ignore
                    oneway: '3',
                }),
            ).toEqual({});
        });

        validOnewayValues.forEach(oneway => {
            it(`содержит валидное значение ${oneway}`, () => {
                expect(
                    sanitizeAviaSearchParams({
                        oneway,
                    }),
                ).toEqual({
                    oneway,
                });
            });
        });
    });

    describe('fromId/toId', () => {
        it('определено только одно значение - вернёт существующее значение', () => {
            expect(
                sanitizeAviaSearchParams({
                    fromId: 'c2',
                }),
            ).toEqual({
                fromId: 'c2',
            });

            expect(
                sanitizeAviaSearchParams({
                    toId: 'c54',
                }),
            ).toEqual({
                toId: 'c54',
            });
        });

        it('fromId/toId определены - вернёт их значения', () => {
            expect(
                sanitizeAviaSearchParams({
                    fromId: 'c213',
                    toId: 'c54',
                }),
            ).toEqual({
                fromId: 'c213',
                toId: 'c54',
            });
        });
    });

    describe('fromName/toName', () => {
        it('определено только одно значение - вернёт существующее значение', () => {
            expect(
                sanitizeAviaSearchParams({
                    fromName: 'Санкт-Петербург',
                }),
            ).toEqual({
                fromName: 'Санкт-Петербург',
            });

            expect(
                sanitizeAviaSearchParams({
                    toName: 'Екатеринбург',
                }),
            ).toEqual({
                toName: 'Екатеринбург',
            });
        });

        it('fromName/toName определены - вернёт их значения', () => {
            expect(
                sanitizeAviaSearchParams({
                    fromName: 'Москва',
                    toName: 'Екатеринбург',
                }),
            ).toEqual({
                fromName: 'Москва',
                toName: 'Екатеринбург',
            });
        });
    });
});
