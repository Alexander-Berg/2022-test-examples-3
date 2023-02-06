'use strict';

const PO = require('./FavoriteButton.page-object');

specs('Яндекс.Избранное', function() {
    describe('Незалогин', function() {
        it('Карточка фильма', async function() {
            await this.browser.yaOpenSerp({
                text: 'Чудо-женщина',
                exp_flags: ['es_favorites_redesign=1', 'rearr=entsearch_experiment=enable-favorites-info;scheme_Local/EntitySearch/MarkFavoriteItems=1'],
                srcskip: 'YABS_DISTR',
            }, PO.FavoriteButton());

            await this.browser.yaMockFetch({
                urlDataMap: {
                    '/collections/api/v1.0/csrf-token': {
                        'csrf-token': 'token',
                    },
                    '/collections/api/v1.0/cards': {
                        id: '62bc67f9024244be9365ea56',
                    },
                },
            });

            await this.browser.yaShouldBeVisible(PO.FavoriteButton(), 'Нет кнопки добавления в избранное');
            await this.browser.assertView('plain', PO.FavoriteButton());

            await this.browser.yaCheckBaobabCounter(PO.FavoriteButton(), {
                path: '/$page/favorite',
            });

            await this.browser.yaWaitForVisible(PO.FavoritesPopup.login(), 3000, 'Тултип с кнопкой логина не появился');
            await this.browser.yaWaitForVisible(PO.FavoriteButton(), 'Кнопка не сменила состояние на "добавлено"');
            await this.browser.assertView('saved-anonym', PO.FavoriteButton());
            await this.browser.yaCheckPassportLink({
                selector: PO.FavoritesPopup.login(),
                origin: 'serp',
            });

            await this.browser.yaRestoreFetch();
        });
    });

    describe('Залогин', function() {
        it('Карточка фильма', async function() {
            await this.browser.yaOpenSerp({
                text: 'Чудо-женщина',
                yandex_login: 'pds.user.no.license',
                exp_flags: ['es_favorites_redesign=1', 'rearr=entsearch_experiment=enable-favorites-info;scheme_Local/EntitySearch/MarkFavoriteItems=1'],
            }, PO.FavoriteButton());

            await this.browser.execute(selector => {
                const el = document.querySelector(selector);
                el && (el.style.display = 'none');
            }, PO.distrPopup());

            await this.browser.yaShouldBeVisible(PO.FavoriteButton(), 'Нет кнопки добавления в избранное');
            await this.browser.assertView('plain', PO.FavoriteButton());

            await this.browser.yaCheckBaobabCounter(PO.FavoriteButton(), {
                path: '/$page/favorite',
            });

            await this.browser.yaWaitForVisible(PO.FavoritesIframe(), 'Попап с iframe не открылся');

            await this.browser.execute(function() {
                $('.Modal-Overlay').css('background', '#000000'); // прячем background
            });
        });
    });
});
