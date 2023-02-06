'use strict';

const PO = require('./Zen_article.page-object').touchPhone;

specs({
    feature: 'Колдунщик Дзена',
    type: 'Аватар и мета блоггера в сниппете статьи',
}, function() {
    describe('Основной вариант', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '2094334613',
                text: 'foreverdata',
                srcskip: 'YABS_DISTR',
                data_filter: 'zen',
            }, PO.zenArticle());
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('zen-article', PO.zenArticle());
        });

        it('Тайтл', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.zenArticle.organic.title.link(),
                baobab: {
                    path: '/$page/$main/$result/title',
                },
                message: 'Неверная ссылка в заголовке',
            });
        });
    });

    describe('Без extended-snippet', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '2676259624',
                text: 'foreverdata',
                srcskip: 'YABS_DISTR',
                data_filter: 'zen',
            }, PO.zenArticle());
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('zen-article-without-extended', PO.zenArticle());
        });
    });

    describe('Обычная фавиконка', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '2676259624',
                text: 'foreverdata',
                srcskip: 'YABS_DISTR',
                data_filter: 'zen',
            }, PO.zenArticle());
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('zen-article-favicon', PO.zenArticle());
        });
    });
});
