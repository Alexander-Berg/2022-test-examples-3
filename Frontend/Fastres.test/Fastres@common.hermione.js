'use strict';

const PO = require('./Fastres.page-object');

specs('Колдунщик миниблендера', function() {
    describe('Сниппет', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '865721240',
                text: 'foreverdata',
                data_filter: 'fastres',
            }, PO.fastres());
        });

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид', async function() {
            await this.browser.assertView('snippet', PO.fastres());
        });

        it('Заголовок', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.fastres.organic.title.link(),
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/title',
                },
            });
        });
    });

    describe('Расширенный сниппет', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '4143172248',
                text: 'foreverdata',
                data_filter: 'fastres',
            }, PO.fastres.extendedText.link());
        });

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид', async function() {
            await this.browser.assertView('extended-snippet', PO.fastres());
        });

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Развернутый', async function() {
            await this.browser.yaCheckBaobabCounter(PO.fastres.extendedText.link(), {
                path: '/$page/$main/$result/extended-text/more[@behaviour@type="dynamic"]',
            });

            await this.browser.assertView('extended-snippet-expanded', PO.fastres());
        });
    });

    describe('С тумбой', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '1189051557',
                text: 'foreverdata',
                data_filter: 'fastres',
            }, PO.fastres.organic.thumb());
        });

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид', async function() {
            await this.browser.assertView('with-thumb', PO.fastres());
        });

        it('Картинка', async function() {
            const platform = await this.browser.getMeta('platform');
            let thumbSelector;
            if (platform !== 'touch-phone' && platform !== 'searchapp-phone') {
                thumbSelector = PO.fastres.organic.thumb();
            } else {
                thumbSelector = PO.fastres.organic.thumbLinkReact();
            }

            await this.browser.yaCheckLink2({
                selector: thumbSelector,
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/thumb',
                },
            });
        });
    });

    describe('Сайтлинки', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '872023881',
                text: 'foreverdata',
                data_filter: 'fastres',
            }, PO.fastres.organic.sitelinks());
        });

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид', async function() {
            await this.browser.assertView('with-sitelinks', PO.fastres());
        });

        it('Сайтлинк', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.fastres.organic.sitelinks.link(),
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/sitelinks/item',
                },
            });
        });
    });

    describe('Сайтлинки в стиле БНО', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '1872426265',
                text: 'foreverdata',
                data_filter: 'fastres',
            }, PO.fastres.sitelinks());
        });

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид', async function() {
            await this.browser.assertView('with-bno-sitelinks', PO.fastres());
        });
    });

    describe('Карусель', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '4184359419',
                text: 'foreverdata',
                data_filter: 'fastres',
            }, PO.fastres.showcase());
        });

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид', async function() {
            await this.browser.assertView('showcase', PO.fastres());
        });

        it('Карточка', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.fastres.showcase.item.link(),
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/showcase/item/thumb',
                },
            });
        });

        it('Тайтл в карточке', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.fastres.showcase.item.descr.link(),
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/showcase/item/subtitle',
                },
            });
        });

        it('Гринурл в карточке', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.fastres.showcase.item.extra.link(),
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/showcase/item/path/urlnav',
                },
            });
        });
    });
});
