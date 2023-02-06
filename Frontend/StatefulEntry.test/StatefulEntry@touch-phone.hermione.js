'use strict';

const PO = require('./StatefulEntry.page-object')('touch-phone');

specs('Точка входа на страницу стейтфула', function() {
    describe('Базовый вариант', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-entry',
                    foreverdata: '3563329814',
                },
                PO.statefulEntry(),
            );
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-entry', PO.statefulEntry());
        });

        hermione.only.notIn(['iphone'], 'orientation is not supported');
        it('Внешний вид в горизонтальной ориентации', async function() {
            await this.browser.setOrientation('landscape');
            await this.browser.assertView('stateful-entry-horiz', PO.statefulEntry());
        });

        it('Ссылка и счетчик', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.statefulEntry.titleLink(),
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/stateful-entry/title',
                    data: { themeId: '2174754698598328073' },
                },
            });
        });
    });

    describe('Базовый вариант с избранным', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-entry',
                    foreverdata: '2980491333',
                },
                PO.statefulEntry(),
            );
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-entry-fav', PO.statefulEntry());
        });

        hermione.only.notIn(['iphone'], 'orientation is not supported');
        it('Внешний вид в горизонтальной ориентации', async function() {
            await this.browser.setOrientation('landscape');
            await this.browser.assertView('stateful-entry-fav-horiz', PO.statefulEntry());
        });
    });

    describe('Базовый вариант с большим количеством избранного', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-entry',
                    foreverdata: '1182070855',
                },
                PO.statefulEntry(),
            );
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-entry-fav', PO.statefulEntry());
        });
    });

    describe('Вариант с фиолетовым сабтайтлом', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-entry',
                    foreverdata: '2634301389',
                },
                PO.statefulEntry(),
            );
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-entry-alice', PO.statefulEntry());
        });
    });

    describe('Вариант с фиолетовым сабтайтлом с большим количеством избранного', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    text: 'foreverdata',
                    data_filter: 'stateful-entry',
                    foreverdata: '2098195610',
                },
                PO.statefulEntry(),
            );
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-entry-alice-fav', PO.statefulEntry());
        });
    });
});
