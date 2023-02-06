import { purifyValue, checkKey } from '../RangeFilter.utils';

describe('RangeFilter.utils', () => {
    describe('purifyValue', () => {
        it('возвращает пустую строку при пустых параметрах', () => {
            expect(purifyValue(undefined)).toEqual('');
            expect(purifyValue('')).toEqual('');
        });
        it('очищает от букв', () => {
            expect(purifyValue('буквы')).toEqual('');
        });
        it('заменяет , на .', () => {
            expect(purifyValue('1,2')).toEqual('1.2');
            expect(purifyValue('1.2')).toEqual('1.2');
        });
        it('убирает пробелы', () => {
            expect(purifyValue('1 222')).toEqual('1222');
            expect(purifyValue('1 222 333')).toEqual('1222333');
        });
        it('убирает лишние разделители', () => {
            expect(purifyValue('1...2')).toEqual('1.2');
            expect(purifyValue('1.,.2')).toEqual('1.2');
            expect(purifyValue('1.')).toEqual('1.');
            expect(purifyValue('1..')).toEqual('1.');
            expect(purifyValue('..1')).toEqual('0.1');
            expect(purifyValue('.')).toEqual('');
            expect(purifyValue('..')).toEqual('');
            expect(purifyValue('1...2.')).toEqual('');
            expect(purifyValue('1.2.3')).toEqual('');
        });
        it('обрабатывает scientific-числа', () => {
            expect(purifyValue('2e+10')).toEqual('20000000000');
        });
        it('обрабатывает 0 в начале', () => {
            expect(purifyValue('.1')).toEqual('0.1');
            expect(purifyValue('0')).toEqual('0');
            expect(purifyValue('01')).toEqual('1');
            expect(purifyValue('001')).toEqual('1');
            expect(purifyValue('0.1')).toEqual('0.1');
            expect(purifyValue('000.1')).toEqual('0.1');
        });
        it('обрабатывает 0 в конце', () => {
            expect(purifyValue('0')).toEqual('0');
            expect(purifyValue('000')).toEqual('0');
            expect(purifyValue('10.0')).toEqual('10.0');
            expect(purifyValue('1.0')).toEqual('1.0');
            expect(purifyValue('1.10')).toEqual('1.10');
            expect(purifyValue('1.01')).toEqual('1.01');
            expect(purifyValue('1.010')).toEqual('1.010');
        });
        it('обрабатывает -', () => {
            expect(purifyValue('-')).toEqual('-');
            expect(purifyValue('-.1')).toEqual('-0.1');
            expect(purifyValue('-1')).toEqual('-1');
            expect(purifyValue('---1')).toEqual('-1');
            expect(purifyValue('-001')).toEqual('-1');
            expect(purifyValue('-1-')).toEqual('');
            expect(purifyValue('.-1')).toEqual('');
            expect(purifyValue('1-')).toEqual('');
            expect(purifyValue('1-2')).toEqual('');
        });
    });

    describe('checkKey', () => {
        describe('Пустая кнопка', () => {
            it('не разрешает вводить что-то странное', () => {
                expect(checkKey('', { value: '' })).toBeFalsy();
            });
        });
        describe('Разделитель', () => {
            it('разрешает вводить разделитель', () => {
                expect(checkKey('.', { value: '' })).toBeTruthy();
            });
            it('не разрешает вводить разделитель, если он есть', () => {
                expect(checkKey('.', { value: '1.1' })).toBeFalsy();
            });
        });
        describe('Цифры', () => {
            it('разрешает вводить цифры', () => {
                [0, 1, 2, 3, 4, 5, 6, 7, 8, 9].forEach(digit => {
                    expect(checkKey(String(digit), { value: '' })).toBeTruthy();
                    expect(checkKey(String(digit), { value: '1.1' })).toBeTruthy();
                });
            });

            it('не разрешает вводить числа', () => {
                expect(checkKey('11', { value: '' })).toBeFalsy();
                expect(checkKey('12', { value: '1.1' })).toBeFalsy();
            });

            it('не разрешает вводить буквы', () => {
                expect(checkKey('a', { value: '' })).toBeFalsy();
                expect(checkKey('a', { value: '1.1' })).toBeFalsy();
            });
        });
        describe('Минус', () => {
            it('разрешает вводить минус', () => {
                expect(checkKey('-', { value: '' })).toBeTruthy();
            });
            it('не разрешает вводить минус, если он есть', () => {
                expect(checkKey('-', { value: '-1' })).toBeFalsy();
            });
        });
        describe('Специальные кнопки', () => {
            const SPEC_KEYS = [
                'ArrowLeft',
                'ArrowRight',
                'ArrowUp',
                'ArrowDown',
                'Backspace',
                'CapsLock',
                'Meta',
                'Shift',
                'Delete',
                'Control',
                'Alt',
                'Enter',
                'Escape',
                'Tab',
                'Insert',
                'Home',
                'End',
            ];

            SPEC_KEYS.forEach(specKey => {
                it(`разрешает нажимать кнопку ${specKey}`, () => {
                    expect(checkKey(specKey, { value: '' })).toBeTruthy();
                    expect(checkKey(specKey, { value: '' })).toBeTruthy();
                });
            });
        });
        describe('Комбинации с зажатыми спец кнопками', () => {
            it('разрешает использовать комбинации с metaKey', () => {
                expect(checkKey('v', { value: '', metaKey: false })).toBeFalsy();
                expect(checkKey('v', { value: '', metaKey: true })).toBeTruthy();
            });
            it('разрешает использовать комбинации с ctrlKey', () => {
                expect(checkKey('v', { value: '', ctrlKey: false })).toBeFalsy();
                expect(checkKey('v', { value: '', ctrlKey: true })).toBeTruthy();
            });
            it('не разрешает использовать комбинации с shiftKey', () => {
                expect(checkKey('v', { value: '', shiftKey: false })).toBeFalsy();
                expect(checkKey('v', { value: '', shiftKey: true })).toBeFalsy();
            });
            it('не разрешает использовать комбинации с altKey', () => {
                expect(checkKey('v', { value: '', altKey: false })).toBeFalsy();
                expect(checkKey('v', { value: '', altKey: true })).toBeFalsy();
            });
        });
    });
});
