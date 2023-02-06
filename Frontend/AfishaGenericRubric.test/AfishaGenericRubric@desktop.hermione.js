'use strict';

const PO = require('./AfishaGenericRubric.page-object');

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

    it('Скролл списка', async function() {
        await this.browser.yaCheckBaobabCounter(PO.afishaGenericRubric.scroller.arrowRight(), {
            path: '/$page/$main/$result/afisha-generic-rubric/scroller/scroll_right[@direction="right"]',
            behaviour: { type: 'dynamic' },
        }, 'Сломан счетчик на стрелке вправо');

        await this.browser.yaWaitForVisible(PO.afishaGenericRubric.scroller.arrowLeft(), 'Стрелка влево не появилась');

        // Если попробовать проскроллить контейнер до того, как окончится анимация -
        // этот скролл будет проигнорирован. Сам подскролл в скроллере написан на JS,
        // и потому не отключается в тестах.
        await this.browser.pause(500);

        await this.browser.yaScrollContainer(PO.afishaGenericRubric.scroller.wrap(), 10000);

        await this.browser.yaWaitForHidden(
            PO.afishaGenericRubric.scroller.arrowRight(),
            'Стрелка вправо не пропала после доскролла до конца галереи',
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

    it('Гринурл', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.afishaGenericRubric.Organic.Path.Link(),
            url: {
                href: 'https://afisha.yandex.ru/moscow/theatre/places/?from=rubric',
                ignore: ['pathname', 'query'],
            },
            baobab: {
                path: '/$page/$main/$result/afisha-generic-rubric/path/urlnav',
            },
            message: 'Сломана ссылка гринурла',
        });
    });
});
