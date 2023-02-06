import {
    renderAssetToString,
    renderElementToString,
} from './render';

describe('Рендеринг в строку', () => {
    it('Корректно рендерит скрипты', () => {
        expect(renderAssetToString({
            type: 'script',
            content: 'console.log("x")',
            attrs: {
                id: 'id',
                async: true
            },
        })).toEqual('<script id="id" async="">console.log("x")</script>');

        expect(renderAssetToString({
            type: 'script',
            url: 'https://script.com',
            attrs: {
                id: 'id',
                async: true
            },
        })).toEqual('<script id="id" async="" src="https://script.com"></script>');
    });

    it('Корректно рендерит стили', () => {
        expect(renderAssetToString({
            type: 'style',
            content: '.x {}',
            attrs: {
                id: 'id',
            },
        })).toEqual('<style id="id">.x {}</style>');

        expect(renderAssetToString({
            type: 'style',
            url: 'https://style.com',
            attrs: {
                id: 'id',
            },
        })).toEqual('<link id="id" href="https://style.com"/>');
    });

    it('Корректно рендерит обычный тэг', () => {
        expect(renderElementToString({
            tag: 'title',
            content: 'title',
            attrs: {
                'data-title': 1,
            },
        })).toEqual('<title data-title="1">title</title>');
    });
});
