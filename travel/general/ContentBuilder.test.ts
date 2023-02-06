import ELanguages from '../../types/ELanguages';

import ContentBuilder from './ContentBuilder';

describe('ContentBuilder', () => {
    let builder: ContentBuilder;

    beforeEach(() => {
        builder = new ContentBuilder({lang: ELanguages.RU});
    });

    test('simple', () => {
        const text = 'Привет';

        expect(builder.compile(text)).toBe('function(){return "Привет";}');
    });

    test('variable', () => {
        const text = 'Мое имя: {{name}}';

        expect(builder.compile(text)).toBe(
            'function(params){return "Мое имя: "+params.name;}',
        );
    });

    test('if', () => {
        const text = 'Заграничный паспорт{{#if number}}: {{number}}{{/if}}';

        expect(builder.compile(text)).toBe(
            'function(params){return "Заграничный паспорт"+(params.number?": "+params.number:"");}',
        );
    });

    test('if/else', () => {
        const text = '{{#if short}}чт{{else}}четверг{{/if}}';

        expect(builder.compile(text)).toBe(
            'function(params){return (params.short?"чт":"четверг");}',
        );
    });

    test('plural', () => {
        const text =
            '{{nights}} {{plural count=nights one="ночь" some="ночи" many="ночей"}}';

        expect(builder.compile(text)).toBe(
            'function(params){return params.nights+" "+(params.nights % 10 === 1 && params.nights % 100 !== 11 ? "ночь" :(params.nights % 10 > 1 && params.nights % 10 < 5 && (params.nights % 100 < 10 || params.nights % 100 > 20) ? "ночи" : "ночей"));}',
        );
    });

    test('nested if', () => {
        const text =
            'У меня {{#if countCats == 1}}всего одна кошка{{else}}{{#if countCats > 3}}много {{else}}несколько {{/if}}кошек{{/if}}';

        expect(builder.compile(text)).toBe(
            'function(params){return "У меня "+((params.countCats==1)?"всего одна кошка":((params.countCats>3)?"много ":"несколько ")+"кошек");}',
        );
    });

    test('complex case', () => {
        const text =
            '{{name}} ребёнок {{#if age < 1}}до 1 года{{else}}{{age}} {{plural count=age one="год" some="года" many="лет"}}{{/if}}';

        expect(builder.compile(text)).toBe(
            'function(params){return params.name+" ребёнок "+((params.age<1)?"до 1 года":params.age+" "+(params.age % 10 === 1 && params.age % 100 !== 11 ? "год" :(params.age % 10 > 1 && params.age % 10 < 5 && (params.age % 100 < 10 || params.age % 100 > 20) ? "года" : "лет")));}',
        );
    });
});
