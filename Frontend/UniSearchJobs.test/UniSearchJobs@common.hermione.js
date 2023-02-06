'use strict';

const PO = require('./UniSearchJobs.page-object');

specs({
    feature: 'Универсальный колдунщик поиска вакансий',
}, function() {
    hermione.also.in('iphone-dark');
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 1893390488,
        }, PO.UniSearchJobs());

        await this.browser.assertView('plain', PO.UniSearchJobs());
    });

    it('Переход по ссылке вакансии', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 1634108701,
        }, PO.UniSearchJobs());

        await this.browser.yaCheckBaobabCounter(PO.UniSearchJobs.Content.List.Item.Container(), {
            path: '/$page/$main/$result/unisearch_vacancies/item/link',
            attrs: {
                title: 'Продуктовый дизайнер',
                url: 'https://hh.ru/path',
            },
        });
    });

    it('Переход по ссылке из списка сайтов', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 1634108701,
        }, PO.UniSearchJobs());

        await this.browser.yaCheckBaobabCounter(PO.UniSearchJobs.Content.List.Item.Links.Link(), {
            path: '/$page/$main/$result/unisearch_vacancies/item/site-link/link',
            attrs: {
                title: 'hh.ru',
                url: 'https://hh.ru/path',
            },
        });
    });
});
