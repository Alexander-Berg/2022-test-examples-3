import {templateReplacement} from 'utilities/templateReplacement';

describe('templateReplacement', () => {
    it('Строка не имеет подстановок - вернёт исходную строку', () => {
        expect(templateReplacement('Hello, world!')).toEqual('Hello, world!');
    });

    it('Строка содержит подстановку, но нет данных - вернёт строку с дефолтным значением', () => {
        expect(
            templateReplacement('Hello, [[#target#world]]!', {greeting: 'Hi'}),
        ).toEqual('Hello, world!');
    });

    it('Строка содержит подстановку, есть данные - вернёт строку с подстановкой', () => {
        expect(
            templateReplacement('[[#greeting#Здравствуй]], [[#target#мир]]!', {
                greeting: 'Бонжур',
                target: 'товарищи',
            }),
        ).toEqual('Бонжур, товарищи!');
    });
});
