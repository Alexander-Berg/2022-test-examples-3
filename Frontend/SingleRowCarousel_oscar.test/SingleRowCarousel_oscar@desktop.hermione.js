'use strict';

const PO = require('./SingleRowCarousel_oscar.page-object');

specs({ feature: 'Однорядная карусель специальных событий', type: 'Оскар' }, () => {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            foreverdata: '2407574761',
            data_filter: 'single-row-carousel',
        }, PO.specialEvent());
    });

    it('Внешний вид', async function() {
        await this.browser.assertView('oscar', PO.specialEvent());
    });

    it('Заголовок', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.specialEvent.reactHeader.title.link(),
            url: {
                href: 'https://www.kinopoisk.ru/special/oscar/nominees/',
            },
            baobab: {
                path: '/$page/$top/$result/special-event-header/title',
            },
        });
    });

    it('Иконка около заголовка', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.specialEvent.reactHeader.icon(),
            url: {
                href: 'https://www.kinopoisk.ru/special/oscar/nominees/',
            },
            baobab: {
                path: '/$page/$top/$result/special-event-header/thumb',
            },
        });
    });

    it('Карточка номинации', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.specialEvent.firstItem.reactShowcase.firstItem.link(),
            url: {
                href: 'https://www.kinopoisk.ru/film/1238506/',
            },
            baobab: {
                path: '/$page/$top/$result/special-event-showcase/showcase/item/link',
            },
        });
    });

    it('Скроллер', async function() {
        await this.browser.yaWaitForVisible(PO.specialEvent.firstItem.reactShowcase.scroller.arrowRight());

        await this.browser.yaCheckBaobabCounter(PO.specialEvent.firstItem.reactShowcase.scroller.arrowRight(), {
            path: '/$page/$top/$result/special-event-showcase/showcase/scroll_right',
        });

        await this.browser.assertView('scrolled-right', PO.specialEvent());

        await this.browser.yaCheckBaobabCounter(PO.specialEvent.firstItem.reactShowcase.scroller.arrowLeft(), {
            path: '/$page/$top/$result/special-event-showcase/showcase/scroll_left',
        });

        await this.browser.assertView('scrolled-left', PO.specialEvent());
    });
});
