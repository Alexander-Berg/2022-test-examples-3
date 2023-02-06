import {CHAR_SIX_PER_EM_SPACE} from 'utilities/strings/charCodes';

import {formatPrice} from '../index';
import {CURRENCY_RUB, CURRENCY_RUR, CURRENCY_USD} from '../codes';

describe('formatPrice({value, currency, isRound})', () => {
    test('Должен вернуть 5 рублей', () => {
        expect(formatPrice({value: 5, currency: CURRENCY_RUB})).toBe(
            `5${CHAR_SIX_PER_EM_SPACE}₽`,
        );
    });

    test('Должен вернуть 5555 рублей', () => {
        expect(formatPrice({value: 5555, currency: CURRENCY_RUB})).toBe(
            `5${CHAR_SIX_PER_EM_SPACE}555${CHAR_SIX_PER_EM_SPACE}₽`,
        );
    });

    test('Должен вернуть 99,99 рублей', () => {
        expect(formatPrice({value: 99.99, currency: CURRENCY_RUB})).toBe(
            `99,99${CHAR_SIX_PER_EM_SPACE}₽`,
        );
    });

    test('Должен вернуть 99,90 рублей', () => {
        expect(formatPrice({value: 99.9, currency: CURRENCY_RUB})).toBe(
            `99,90${CHAR_SIX_PER_EM_SPACE}₽`,
        );
    });

    test('Должен вернуть 5 долларов', () => {
        expect(formatPrice({value: 5, currency: CURRENCY_USD})).toBe(`$5`);
    });

    test('Должен вернуть 5555 долларов', () => {
        expect(formatPrice({value: 5555, currency: CURRENCY_USD})).toBe(
            `$5${CHAR_SIX_PER_EM_SPACE}555`,
        );
    });

    test('Должен вернуть 99,99 долларов', () => {
        expect(formatPrice({value: 99.99, currency: CURRENCY_USD})).toBe(
            `$99.99`,
        );
    });

    test('Должен вернуть 99,90 долларов', () => {
        expect(formatPrice({value: 99.9, currency: CURRENCY_USD})).toBe(
            `$99.90`,
        );
    });

    test('Должен округлить значение в большую сторону', () => {
        expect(
            formatPrice({value: 99.5, currency: CURRENCY_RUB, isRound: true}),
        ).toBe(`100${CHAR_SIX_PER_EM_SPACE}₽`);
    });

    test('Должен округлить значение в меньшую сторону', () => {
        expect(
            formatPrice({value: 99.49, currency: CURRENCY_RUB, isRound: true}),
        ).toBe(`99${CHAR_SIX_PER_EM_SPACE}₽`);
    });

    test('Должен убрать из значения нулевые дробные числа', () => {
        expect(formatPrice({value: 5.0, currency: CURRENCY_RUB})).toBe(
            `5${CHAR_SIX_PER_EM_SPACE}₽`,
        );
    });

    test('Должен вернуть 5 рублей для валюты RUR', () => {
        expect(formatPrice({value: 5, currency: CURRENCY_RUR})).toBe(
            `5${CHAR_SIX_PER_EM_SPACE}₽`,
        );
    });

    test('Должен вернуть 5 рублей для значения "5", заданного строкой', () => {
        expect(formatPrice({value: '5', currency: CURRENCY_RUB})).toBe(
            `5${CHAR_SIX_PER_EM_SPACE}₽`,
        );
    });

    test('Должен вернуть 5,11 рублей для значения "5.1111", заданного строкой', () => {
        expect(formatPrice({value: '5.1111', currency: CURRENCY_RUR})).toBe(
            `5,11${CHAR_SIX_PER_EM_SPACE}₽`,
        );
    });

    test('Должен вернуть значение и код для неизвестной валюты', () => {
        // @ts-ignore
        expect(formatPrice({value: 5, currency: 'INVALID'})).toBe(
            `5${CHAR_SIX_PER_EM_SPACE}INVALID`,
        );
    });
});
