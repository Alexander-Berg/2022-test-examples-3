'use strict';

const PO = require('./UniSearchApps.page-object');
const { openPreview } = require('./helpers');

specs({
    feature: 'Универсальный колдунщик поиска приложений',
}, function() {
    hermione.also.in('iphone-dark');
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 1177338420,
        }, PO.UniSearchApps());

        await this.browser.assertView('plain', PO.UniSearchApps());
    });

    it('Внешний вид play.google.com', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 458941406,
        }, PO.UniSearchApps());

        await this.browser.assertView('plain_without_more', PO.UniSearchApps());
    });

    it('Внешний вид AppGallery', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 1232199508,
        }, PO.UniSearchApps());

        await this.browser.assertView('plain_without_more', PO.UniSearchApps());
    });

    it('Переход по ссылке приложения', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 1177338420,
        }, PO.UniSearchApps());

        await this.browser.yaCheckCounter2(PO.UniSearchApps.Content.List.Item.MainLink(), {
            url: 'https://yandex.ru/search/?text=%D0%A1%D0%B5%D0%BA%D1%83%D0%BD%D0%B4%D0%BE%D0%BC%D0%B5%D1%80',
        });
        await this.browser.yaCheckBaobabCounter(PO.UniSearchApps.Content.List.Item.MainLink(), {
            path: '/$page/$main/$result/unisearch_applications/item/link',
            attrs: {
                url: 'https://yandex.ru/search/?text=Секундомер',
                title: 'С большим кол-вом отзывов',
                isFree: true,
                rating: 1.7,
                reviews: '1M отзывов',
            },
        });
    });

    describe('Обогащённое превью', function() {
        const serpParams = {
            text: 'foreverdata',
            foreverdata: 2490704819,
            exp_flags: 'unisearch_app_preview',
        };
        hermione.only.notIn('searchapp-phone', 'https://st.yandex-team.ru/SERP-141914');
        it('Внешний вид', async function() {
            await openPreview(this, serpParams);
            // Убираем незначительные для скриншота элементы выдачи под превью,
            // чтобы их изменения не затрагивали тест.
            await this.browser.execute(() => {
                [
                    document.querySelector('.main'),
                    document.querySelector('.serp-footer'),
                    document.querySelector('.serp-header'),
                    document.querySelector('.HeaderPhone'),
                    document.querySelector('.serp-navigation'),
                ].filter(Boolean).forEach(node => {
                    node.style.display = 'none';
                });
            });
            await this.browser.assertView('plain', PO.UniSearchPreview());
        });
    });
});
