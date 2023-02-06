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
            data_filter: 'afisha-generic-rubric',
        }, PO.afishaGenericRubric());
    });

    hermione.also.in('safari13');
    it('Скролл списка', async function() {
        await this.browser.yaCheckBaobabCounter(
            () => this.browser.yaScrollContainer(PO.afishaGenericRubric.scroller.wrap(), 10000),
            {
                event: 'scroll',
                path: '/$page/$main/$result/afisha-generic-rubric/scroller',
                behaviour: { type: 'dynamic' },
            },
            'Сломан счетчик скролла',
        );

        await this.browser.assertView('more', [
            PO.afishaGenericRubric.scroller(),
            PO.afishaGenericRubric.more(),
        ]);

        await this.browser.yaCheckLink2({
            selector: PO.afishaGenericRubric.more.link(),
            url: {
                href: 'https://afisha.yandex.ru/moscow/theatre/places/?from=rubric',
                ignore: ['pathname', 'query'],
            },
            baobab: {
                path: '/$page/$main/$result/afisha-generic-rubric/scroller/more',
            },
            message: 'Сломана ссылка на полный список в конце списка',
        });
    });
});
