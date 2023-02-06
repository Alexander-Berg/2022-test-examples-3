import required from '../required';
import avaliable from '../avaliable';

describe('Common validation rules', () => {
    test('Отключенное правило required возвращает true', () => {
        expect(required(false, undefined)).toBeTruthy();
    });

    test('Правило required возвращает true для не пустой строки', () => {
        expect(required(true, '123')).toBeTruthy();
    });

    test('Правило required возвращает true для number больше нуля', () => {
        expect(required(true, 1)).toBeTruthy();
    });

    test('Правило required возвращает true для объекта', () => {
        expect(required(true, {})).toBeTruthy();
    });

    test('Правило required возвращает false для пустой строки', () => {
        expect(required(true, '')).toBeFalsy();
    });

    test('Правило required возвращает false для number равному нулю', () => {
        expect(required(true, 0)).toBeFalsy();
    });

    test('Правило required возвращает false для undefined', () => {
        expect(required(true, undefined)).toBeFalsy();
    });

    test('Правило required возвращает false для null', () => {
        expect(required(true, null)).toBeFalsy();
    });

    test('Отключенное правило avaliable возвращает true', () => {
        expect(avaliable(true, 'Some value')).toBeTruthy();
    });

    test('Правило avaliable возвращает false для строки', () => {
        expect(avaliable(false, '')).toBeTruthy();
    });

    test('Правило avaliable возвращает true для number равному нулю', () => {
        expect(avaliable(true, 0)).toBeTruthy();
    });

    test('Правило avaliable возвращает true для undefined', () => {
        expect(avaliable(true, undefined)).toBeTruthy();
    });

    test('Правило avaliable возвращает true для null', () => {
        expect(avaliable(true, null)).toBeTruthy();
    });

    test('Правило avaliable возвращает false для строки', () => {
        expect(avaliable(false, 'Some value')).toBeFalsy();
    });

    test('Правило avaliable возвращает false для number больше нуля', () => {
        expect(avaliable(false, 1)).toBeFalsy();
    });

    test('Правило avaliable возвращает false для объекта', () => {
        expect(avaliable(false, {})).toBeFalsy();
    });
});
