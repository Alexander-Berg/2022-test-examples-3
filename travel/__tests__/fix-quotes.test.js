import {split, build, fixQuotes} from '../fix-quotes';

const stringWithQuotes = 'станция "Олупка"';
const stringWithNestedQuotes = 'станция "Олупка \'север\'"';
const stringWithLongNestedQuotes = 'станция "Олупка \'север «северный»\'"';

const splitedStringWithQuotes = ['станция ', ['Олупка']];
const splitedStringWithNestedQuotes = ['станция ', ['Олупка ', ['север']]];
const splitedStringWithLongNestedQuotes = [
    'станция ',
    ['Олупка ', ['север ', ['северный']]],
];

const builtStringWithQuotes = 'станция «Олупка»';
const builtStringWithNestedQuotes = 'станция «Олупка „север“»';
const builtStringWithLongNestedQuotes = 'станция «Олупка „север «северный»“»';

describe('split', () => {
    it('Должна разбить строку на массив из вложенных скобочных групп', () => {
        expect(split(stringWithQuotes)).toEqual(splitedStringWithQuotes);
        expect(split(stringWithNestedQuotes)).toEqual(
            splitedStringWithNestedQuotes,
        );
        expect(split(stringWithLongNestedQuotes)).toEqual(
            splitedStringWithLongNestedQuotes,
        );
    });
});

describe('build', () => {
    it('Должна вернуть строку с правильным приоритетом скобок', () => {
        expect(build(splitedStringWithQuotes, 0)).toBe(builtStringWithQuotes);
        expect(build(splitedStringWithNestedQuotes, 0)).toBe(
            builtStringWithNestedQuotes,
        );
        expect(build(splitedStringWithLongNestedQuotes, 0)).toBe(
            builtStringWithLongNestedQuotes,
        );
    });

    it('Должна вернуть строку с обратным приоритетом скобок', () => {
        expect(build(splitedStringWithQuotes, 1)).toBe('станция „Олупка“');
        expect(build(splitedStringWithNestedQuotes, 1)).toBe(
            'станция „Олупка «север»“',
        );
        expect(build(splitedStringWithLongNestedQuotes, 1)).toBe(
            'станция „Олупка «север „северный“»“',
        );
    });
});

describe('fixQuotes', () => {
    it('Должна вернуть неизменившуюся строку', () => {
        expect(fixQuotes('Станция')).toBe('Станция');
    });

    it('Должна удалить пробелы в начали и конце строки', () => {
        expect(fixQuotes(' Станция  ')).toBe('Станция');
    });

    it('Должна удалить ненужные кавычки', () => {
        expect(fixQuotes('"Станция"')).toBe('Станция');
    });

    it('Должна вернуть строку с расставленными кавычками', () => {
        expect(fixQuotes(stringWithQuotes)).toBe(builtStringWithQuotes);
        expect(fixQuotes(stringWithNestedQuotes)).toBe(
            builtStringWithNestedQuotes,
        );
        expect(fixQuotes(stringWithLongNestedQuotes)).toBe(
            builtStringWithLongNestedQuotes,
        );
    });
});
