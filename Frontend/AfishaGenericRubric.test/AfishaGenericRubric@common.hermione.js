'use strict';

const PO = require('./AfishaGenericRubric.page-object/');

specs({
    feature: 'Афиша',
    type: 'Рубричный',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '1165583593',
        }, PO.afishaGenericRubric());
    });

    it('Заголовок', async function() {
        await this.browser.assertView('afisha', [
            PO.afishaGenericRubric(),
            PO.afishaGenericRubric.Organic.Favicon(),
        ]);

        await this.browser.yaCheckLink2({
            selector: PO.afishaGenericRubric.Organic.Title.Link(),
            url: {
                href: 'https://afisha.yandex.ru/moscow/theatre/places/?from=rubric',
                ignore: ['pathname', 'query'],
            },
            baobab: {
                path: '/$page/$main/$result/afisha-generic-rubric/title',
            },
            message: 'Сломана ссылка на заголовке',
        });
    });

    it('Элемент списка', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.afishaGenericRubric.firstItem.link(),
            url: {
                href: 'https://afisha.yandex.ru/yekaterinburg/musical-play/sashbash-sverdlovsk-leningrad-i-nazad',
                ignore: ['pathname', 'query'],
            },
            baobab: {
                path: '/$page/$main/$result/afisha-generic-rubric/scroller/link',
            },
            message: 'Сломана ссылка на первом элементе списка',
        });
    });
});
