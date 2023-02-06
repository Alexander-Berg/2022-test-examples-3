import {inlineStylesToObject} from 'projects/journal/utilities/markup/inlineStylesToObject';

describe('inlineStylesToObject', () => {
    it('Вернёт объект стилей соответствующий инлайн стилям', () => {
        expect(
            inlineStylesToObject(
                'text-align:center; background:  no-repeat url("https://travel.yandex.ru/favicon.ico")',
            ),
        ).toEqual({
            textAlign: 'center',
            background: 'no-repeat url("https://travel.yandex.ru/favicon.ico")',
        });
    });
});
