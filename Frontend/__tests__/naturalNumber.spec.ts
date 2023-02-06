import { naturalNumber } from '../naturalNumber';

test('Обрезает нецифровые символы', () => {
    const value = naturalNumber('10a3g+45-6789.012,34567');

    expect(value).toEqual('10345678901234567');
});

test('Удаляет лишние 0 в начале', () => {
    const value = naturalNumber('00040523');

    expect(value).toEqual('40523');
});

test('Оставляет 0 если строка состоит только из одного 0', () => {
    const value = naturalNumber('0');

    expect(value).toEqual('0');
});
