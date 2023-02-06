'use strict';

const PO = require('./Fastres.page-object');

specs('Колдунщик миниблендера', function() {
    describe('Карусель с текстом внутри карточки', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '683525429',
                text: 'foreverdata',
                data_filter: 'fastres',
            }, PO.fastres.showcase());
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('showcase-over', PO.fastres());
        });
    });

    describe('Сниппет с приложением', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '1931361897',
                text: 'foreverdata',
                data_filter: 'fastres',
            }, PO.fastres());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('with-app', PO.fastres());
        });

        it('Ссылка на блоке с приложением', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.fastres.app(),
                target: '_self',
                baobab: {
                    path: '/$page/$main/$result/bno/app/link',
                },
            });
        });
    });
});
