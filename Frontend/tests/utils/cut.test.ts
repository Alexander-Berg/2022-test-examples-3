import { cut } from '../../utils/cut';

describe('TextCut/utils/cut', () => {
    it('не обрезает текст без указания параметров', () => {
        const text = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.';
        const short = cut(text, {});
        expect(short).toBe(text);
    });

    it('не обрезает короткий текст', () => {
        const text = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.';
        const short = cut(text, { maxLength: 200 });
        expect(short).toBe(text);
    });

    it('обрезает текст по количеству символов', () => {
        const text = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.';

        const short = cut(text, { maxLength: 100 });
        expect(short).toBe('Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore');
    });

    it('обрезает текст по количеству переносов строк', () => {
        const text = `Lorem ipsum
dolor sit amet,
consectetur adipiscing elit,
sed do eiusmod tempor
incididunt ut labore
et dolore magna aliqua.`;

        const short = cut(text, { maxLineBreaks: 3 });
        expect(short).toBe(`Lorem ipsum
dolor sit amet,
consectetur adipiscing elit`);
    });

    it('обрезает текст по меньшему ограничению в количество символов', () => {
        const text = `Lorem ipsum
dolor sit amet,
consectetur adipiscing elit,
sed do eiusmod tempor
incididunt ut labore
et dolore magna aliqua.`;

        const short = cut(text, { maxLength: 50, maxLineBreaks: 5 });
        expect(short).toBe(`Lorem ipsum
dolor sit amet,
consectetur adipiscing`);
    });

    it('обрезает текст по меньшему ограничению в количество переносов строк', () => {
        const text = `Lorem ipsum dolor sit amet,
consectetur adipiscing elit, sed do eiusmod tempor
incididunt ut labore et dolore magna aliqua.`;

        const short = cut(text, { maxLength: 100, maxLineBreaks: 2 });
        expect(short).toBe(`Lorem ipsum dolor sit amet,
consectetur adipiscing elit, sed do eiusmod tempor`);
    });

    it('не оставляет в конце пробелы', () => {
        const text = 'Lorem ipsum dolor   sit amet.';
        const short = cut(text, { maxLength: 20, maxOverLength: 5 });
        expect(short).toBe('Lorem ipsum dolor');
    });

    it('не обрезает один символ в конце текста', () => {
        const text = 'Lorem ipsum dolor sit amet.';
        const short = cut(text, { maxLength: 26 });
        expect(short).toBe(text);
    });

    it('не обрезает символы в рамках допустимого количества', () => {
        const text = 'Lorem ipsum dolor sit amet.';
        const short = cut(text, { maxLength: 18, maxOverLength: 9 });
        expect(short).toBe(text);
    });

    it('не оставляет в конце точку', () => {
        const text = 'Lorem ipsum dolor sit amet. Consectetur adipiscing elit';
        const short = cut(text, { maxLength: 27, maxOverLength: 5 });
        expect(short).toBe('Lorem ipsum dolor sit amet');
    });

    it('не оставляет в конце запятую', () => {
        const text = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit';
        const short = cut(text, { maxLength: 27, maxOverLength: 5 });
        expect(short).toBe('Lorem ipsum dolor sit amet');
    });

    it('не оставляет в конце вопросительный знак', () => {
        const text = 'Lorem ipsum dolor sit amet? Consectetur adipiscing elit';
        const short = cut(text, { maxLength: 27, maxOverLength: 5 });
        expect(short).toBe('Lorem ipsum dolor sit amet');
    });

    it('не оставляет в конце восклицательный знак', () => {
        const text = 'Lorem ipsum dolor sit amet! Consectetur adipiscing elit';
        const short = cut(text, { maxLength: 27, maxOverLength: 5 });
        expect(short).toBe('Lorem ipsum dolor sit amet');
    });

    it('не оставляет в конце двоеточие', () => {
        const text = 'Lorem ipsum dolor sit amet: Consectetur adipiscing elit';
        const short = cut(text, { maxLength: 27, maxOverLength: 5 });
        expect(short).toBe('Lorem ipsum dolor sit amet');
    });

    it('не оставляет пробелы после сокращения знака препинания в конце', () => {
        const text = 'Lorem ipsum dolor sit amet ! Consectetur adipiscing elit';
        const short = cut(text, { maxLength: 28, maxOverLength: 5 });
        expect(short).toBe('Lorem ipsum dolor sit amet');
    });

    it('не разбивает суррогатную пару', () => {
        const text = 'Hello💚 perfect world!';

        const short = cut(text, { maxLength: 6, maxOverLength: 5 });
        expect(short).toBe('Hello');
    });
});
