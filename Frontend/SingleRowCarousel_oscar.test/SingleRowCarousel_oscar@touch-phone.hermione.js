'use strict';

const PO = require('./SingleRowCarousel_oscar.page-object');

specs({ feature: 'Однорядная карусель специальных событий', type: 'Оскар' }, () => {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            foreverdata: '786632608',
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
                path: '/$page/$main/$result/special-event-header/title',
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
                path: '/$page/$main/$result/special-event-header/thumb',
            },
        });
    });

    it('Карточка номинации', async function() {
        const PO = this.PO;

        await this.browser.yaCheckLink2({
            selector: PO.specialEvent.firstItem.reactShowcase.firstItem.link(),
            url: {
                href: 'https://www.kinopoisk.ru/film/1238506/',
            },
            baobab: {
                path: '/$page/$main/$result/special-event-showcase/showcase/item/link',
            },
        });
    });
});
