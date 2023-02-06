'use strict';

specs({
    feature: 'Колдунщик Авто.ру',
    type: 'С витриной',
}, function() {
    const origin = 'https://m.auto.ru';
    const ignore = ['protocol', 'query', 'hash'];

    describe('Вид без цен', function() {
        beforeEach(async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsText;

            await this.browser.yaOpenSerp({
                foreverdata: '888883305',
                data_filter: 'autoru-thumbs-text' }, wizard());
        });

        it('Заголовок', async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsText;

            await this.browser.assertView('plain', wizard());

            await this.browser.yaCheckLink2({
                selector: wizard.title.link(),
                url: {
                    href: `${origin}/moskva/cars/all/`,
                    ignore,
                },
                baobab: {
                    path: '/$page/$main/$result/title',
                },
            });
        });

        it('Ссылки в тумбах', async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsText;

            await this.browser.yaCheckLink2({
                selector: wizard.showcase.firstItemThumbLink(),
                url: {
                    href: `${origin}/moskva/cars/audi/all/`,
                    ignore,
                },
                baobab: {
                    path: '/$page/$main/$result/showcase/item/thumb',
                },
            });

            await this.browser.yaCheckLink2({
                selector: wizard.showcase.firstItem1DescrItemLink(),
                url: {
                    href: `${origin}/moskva/cars/audi/all/`,
                    ignore,
                },
                baobab: {
                    path: '/$page/$main/$result/showcase/item/subtitle',
                },
            });

            await this.browser.yaCheckLink2({
                selector: wizard.showcase.firstItem2DescrItemLink(),
                url: {
                    href: `${origin}/moskva/cars/audi/all/`,
                    ignore,
                },
                baobab: {
                    path: '/$page/$main/$result/showcase/item/text',
                },
            });
        });

        it('Кнопка Больше автомобилей', async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsText;

            await this.browser.yaScrollContainer(wizard.showcase.scrollWrap(), 99999);
            await this.browser.assertView('scrolled', wizard());

            await this.browser.yaCheckLink2({
                selector: wizard.showcase.moreThumbLink(),
                url: {
                    href: `${origin}/moskva/cars/all/`,
                    ignore,
                },
                baobab: {
                    path: '/$page/$main/$result/showcase/more',
                },
            });
        });
    });
});
