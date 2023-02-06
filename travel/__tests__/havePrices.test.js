jest.disableAutomock();

import havePrices from '../havePrices';

describe('havePrices', () => {
    describe('Позитивные', () => {
        it('Вернет `true`, если в сегменте есть по крайней мере один сформированный тариф', () => {
            const segment = {
                tariffs: {
                    classes: {
                        compartment: 1000,
                        platzkarte: 2000,
                    },
                },
            };

            expect(havePrices(segment)).toBe(true);
        });
    });

    describe('Обработка отсутствия пути до объекта', () => {
        it('`tariffs.classes` пуст', () => {
            const segment = {
                tariffs: {
                    classes: {},
                },
            };

            expect(havePrices(segment)).toBe(false);
        });

        it('`tariffs.classes` не существует', () => {
            const segment = {
                tariffs: {},
            };

            expect(havePrices(segment)).toBe(false);
        });

        it('`tariffs` не существует', () => {
            const segment = {};

            expect(havePrices(segment)).toBe(false);
        });
    });
});
