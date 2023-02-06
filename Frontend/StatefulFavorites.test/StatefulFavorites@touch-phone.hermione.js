'use strict';

const PO = require('./StatefulFavorites.page-object')('touch-phone');

const mockData = require('./test-stubs/mockData');
const mockOptions = {
    urlDataMap: {
        'topic/ajax': mockData,
    },
};

specs('Колдунщик stateful favorites сценария', function() {
    it('Колдунщик избранного', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            data_filter: 'stateful-favorites',
            foreverdata: '888010629',
        }, PO.statefulFavorites());

        await this.browser.yaCheckLink2({
            selector: PO.statefulFavorites.scroller.firstCard.link(),
            baobab: { path: '/$page/$main/$result/stateful-favorites/scroller/stateful-favorite-card/link' },
        });
    });

    it('Колдунщик избранного с текстовой карточкой', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            data_filter: 'stateful-favorites',
            foreverdata: '2697038701',
        }, PO.statefulFavorites());

        await this.browser.assertView('stateful-favorites-with-text-card', PO.statefulFavorites());
    });

    hermione.also.in('iphone-dark');
    it('Колдунщик избранного и шторка', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            data_filter: 'stateful-favorites',
            foreverdata: '888010629',
        }, PO.statefulFavorites());

        await this.browser.assertView('stateful-favorites', PO.statefulFavorites());
        await this.browser.yaCheckBaobabCounter(
            PO.statefulFavoritesTitle(),
            { path: '/$page/$main/$result/stateful-favorites/title' },
        );

        await this.browser.yaWaitForVisible(PO.statefulFavoritesDrawer.Content());

        await this.browser.assertView('stateful-favorites-drawer', PO.statefulFavoritesDrawer.Content());

        await this.browser.yaCheckLink2({
            selector: PO.statefulFavoritesDrawer.Content.firstCard.link(),
            baobab: { path: '/$page/$main/$result/stateful-favorites/stateful-favorite-card/link' },
        });
    });

    describe('Догрузка карточек в шторке', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful-favorites',
                foreverdata: '1391261080',
            }, PO.statefulFavorites());

            await this.browser.yaCheckBaobabCounter(
                PO.statefulFavoritesTitle(),
                { path: '/$page/$main/$result/stateful-favorites/title' },
            );

            await this.browser.yaWaitForVisible(PO.statefulFavoritesDrawer.Content());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.yaWaitForVisible(PO.statefulFavoritesDrawer.Content.button());
            await this.browser.assertView('stateful-favorites-drawer', PO.statefulFavoritesDrawer.Content());
        });

        it('Догрузка по нажатию на кнопку', async function() {
            await this.browser.yaWaitForVisible(PO.statefulFavoritesDrawer.Content.button());

            await this.browser.yaMockXHR(mockOptions);

            await this.browser.yaCheckBaobabCounter(
                PO.statefulFavoritesDrawer.Content.button(),
                { path: '/$page/$main/$result/stateful-favorites/load-more' },
            );

            await this.browser.yaWaitForHidden(PO.statefulFavoritesDrawer.Content.button());
            await this.browser.yaWaitForHidden(PO.statefulFavoritesDrawer.Content.spin());

            await this.browser.yaStubImage(PO.statefulFavoritesDrawer.Feed.Image());

            await this.browser.assertView('stateful-favorites-drawer-full', PO.statefulFavoritesDrawer.Content());
        });
    });

    hermione.also.in('iphone-dark');
    it('Редизайн', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            data_filter: 'stateful-favorites',
            foreverdata: '41053474',
        }, PO.statefulFavorites());

        await this.browser.assertView('stateful-favorites', PO.statefulFavorites());
        await this.browser.yaCheckBaobabCounter(
            PO.statefulFavoritesTitle(),
            { path: '/$page/$main/$result/stateful-favorites/title' },
        );

        await this.browser.yaWaitForVisible(PO.statefulFavoritesDrawer.Content());

        await this.browser.assertView('stateful-favorites-drawer', PO.statefulFavoritesDrawer.Content());

        await this.browser.yaCheckLink2({
            selector: PO.statefulFavoritesDrawer.Content.firstCard.link(),
            baobab: { path: '/$page/$main/$result/stateful-favorites/stateful-favorite-card/link' },
        });
    });

    hermione.only.notIn(['iphone'], 'orientation is not supported');
    it('Редизайн - горизонтальная ориентация', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            data_filter: 'stateful-favorites',
            foreverdata: '41053474',
        }, PO.statefulFavorites());

        await this.browser.setOrientation('landscape');
        await this.browser.assertView('stateful-favorites-land', PO.statefulFavorites());
        await this.browser.click(PO.statefulFavoritesTitle());
        await this.browser.yaWaitForVisible(PO.statefulFavoritesDrawer.Content());
        await this.browser.assertView('stateful-favorites-drawer-land', PO.statefulFavoritesDrawer.Content());
    });

    hermione.also.in('iphone-dark');
    it('Редизайн - без избранного', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            data_filter: 'stateful-favorites',
            foreverdata: '2004700583',
        }, PO.statefulFavorites());

        await this.browser.assertView('stateful-favorites-empty', PO.statefulFavorites());
    });
});
