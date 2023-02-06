'use strict';
const { yaCheckBaobabServerCounter } = require('../../CostPlus/CostPlus.test/helpers');

specs({
    feature: 'Колдунщик Авто.ру',
    type: 'С витриной',
    experiment: 'Органикализация',
}, function() {
    hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2780918842',
            data_filter: 'autoru/thumbs-price',
        }, this.PO.autoTypeThumbsPrice());

        await this.browser.assertView('plain', this.PO.autoTypeThumbsPrice());
    });

    it('Внешний вид с большим количеством карточек', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2675294810',
            data_filter: 'autoru/thumbs-price',
        }, this.PO.autoTypeThumbsPrice());

        await this.browser.assertView('big-list', this.PO.autoTypeThumbsPrice());
    });

    it('Внешний вид с WEB_NAV (recommendation_label)', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '1483370846',
            data_filter: 'autoru/thumbs-price',
        }, this.PO.autoTypeThumbsPrice());

        await this.browser.assertView('with_recommendation_label', this.PO.autoTypeThumbsPrice());
    });

    it('Разметка Baobab', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2675294810',
            data_filter: 'empty',
        }, this.PO.autoTypeThumbsPrice());

        await yaCheckBaobabServerCounter.call(this.browser, {
            path: '/$page/$main/$result/costplus[@type="auto"]',
        });

        await yaCheckBaobabServerCounter.call(this.browser, {
            path: '/$page/$main/$result/costplus[@type="auto"]/data[@type="gallery"]',
        });
        await yaCheckBaobabServerCounter.call(this.browser, {
            path: '/$page/$main/$result/costplus[@type="auto"]/data[@type="gallery"]/item',
            attrs: {
                item: '0',
                url: 'https://auto.ru/moskva/cars/bmw/3er/all/?pinned_offer_id=autoru-1101173786',
            },
        });
        await yaCheckBaobabServerCounter.call(this.browser, {
            path: '/$page/$main/$result/costplus[@type="auto"]/data[@type="gallery"]/more_link',
            attrs: {
                url: 'https://auto.ru/moskva/cars/bmw/3er/used/',
            },
        });
    });
});
