'use strict';

specs({
    feature: 'Колдунщик Авто.ру',
    type: 'С витриной',
}, function() {
    const origin = 'https://auto.ru';
    const ignore = ['protocol', 'query'];

    describe('Вид с ценами', function() {
        hermione.also.in(['firefox', 'ipad']);
        it('Внешний вид', async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsPrice;

            await this.browser.yaOpenSerp({
                foreverdata: '1825085094',
                data_filter: 'autoru-thumbs-price',
            }, wizard());

            await this.browser.yaAssertViewExtended('plain', wizard(), {
                hideElements: [PO.popup2()],
                horisontalOffset: 26,
                verticalOffset: 0,
            });

            await this.browser.setCookie({
                name: 'yp',
                value: '1660603667.szm.2%3A1920x1080%3A1920x1080.',
            });

            await this.browser.yaWaitUntilSerpReloaded(() => this.browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: '1825085094',
                data_filter: 'autoru-thumbs-price',
            }, wizard()));

            await this.browser.yaAssertViewExtended('cookie', wizard(), {
                hideElements: [PO.popup2()],
                horisontalOffset: 26,
                verticalOffset: 0,
            });
        });

        hermione.also.in('chrome-desktop-1920');
        hermione.only.in('chrome-desktop-1920');
        it('Широкие десктопы', async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsPrice;

            await this.browser.yaOpenSerp({
                foreverdata: '1825085094',
                data_filter: 'autoru-thumbs-price',
            }, wizard());

            await this.browser.yaAssertViewExtended('wide', wizard(), {
                hideElements: [PO.popup2()],
                horisontalOffset: 26,
                verticalOffset: 0,
            });
        });

        it('Внешний вид без сайтлинков', async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsPrice;

            await this.browser.yaOpenSerp({
                foreverdata: '2559287519',
                data_filter: 'autoru-thumbs-price',
            }, wizard());

            await this.browser.yaAssertViewExtended('no-sitelinks', wizard(), {
                hideElements: [PO.popup2()],
                horisontalOffset: 26,
                verticalOffset: 0,
            });
        });

        describe('Cсылки и скролл', function() {
            beforeEach(async function() {
                const PO = this.PO;
                const wizard = PO.autoTypeThumbsPrice;

                await this.browser.yaOpenSerp({
                    foreverdata: '1825085094',
                    data_filter: 'autoru-thumbs-price',
                }, wizard());
            });

            it('Заголовок', async function() {
                const PO = this.PO;
                const wizard = PO.autoTypeThumbsPrice;

                await this.browser.yaCheckLink2({
                    selector: wizard.title.link(),
                    url: {
                        href: `${origin}/moskva/cars/kia/rio/all/`,
                        ignore,
                    },
                    baobab: {
                        path: '/$page/$main/$result/title',
                    },
                });
            });

            it('Гринурл', async function() {
                const PO = this.PO;
                const wizard = PO.autoTypeThumbsPrice;

                await this.browser.yaCheckLink2({
                    selector: wizard.greenurl.link(),
                    url: {
                        href: `${origin}/moskva/cars/kia/rio/all/`,
                        ignore,
                    },
                    baobab: {
                        path: '/$page/$main/$result/path/urlnav',
                    },
                });
            });

            it('Ссылка на отзывы', async function() {
                const PO = this.PO;
                const wizard = PO.autoTypeThumbsPrice;

                await this.browser.yaCheckLink2({
                    selector: wizard.sitelinks.first(),
                    url: {
                        href: 'https://media.auto.ru/reviews/cars/kia/rio/',
                        ignore,
                    },
                    baobab: {
                        path: '/$page/$main/$result/sitelinks/item[@pos=0]',
                    },
                });
            });

            it('Ссылка на характеристики', async function() {
                const PO = this.PO;
                const wizard = PO.autoTypeThumbsPrice;

                await this.browser.yaCheckLink2({
                    selector: wizard.sitelinks.second(),
                    url: {
                        href: `${origin}/catalog/cars/kia/rio/22500704/22500752/specifications/`,
                        ignore,
                    },
                    baobab: {
                        path: '/$page/$main/$result/sitelinks/item[@pos=1]',
                    },
                });
            });

            it('Ссылка на комплектации', async function() {
                const PO = this.PO;
                const wizard = PO.autoTypeThumbsPrice;

                await this.browser.yaCheckLink2({
                    selector: wizard.sitelinks.third(),
                    url: {
                        href: `${origin}/catalog/cars/kia/rio/22500704/22500752/equipment/`,
                        ignore,
                    },
                    baobab: {
                        path: '/$page/$main/$result/sitelinks/item[@pos=2]',
                    },
                });
            });

            it('Первая тумба витрины', async function() {
                const PO = this.PO;
                const wizard = PO.autoTypeThumbsPrice;

                await this.browser.yaCheckLink2({
                    selector: wizard.showcase.firstItemThumbLink(),
                    url: {
                        href: `${origin}/moskva/cars/kia/rio/new/`,
                        ignore,
                    },
                    baobab: {
                        path: '/$page/$main/$result/showcase/item/thumb',
                    },
                });
            });

            it('Вторая тумба витрины', async function() {
                const PO = this.PO;
                const wizard = PO.autoTypeThumbsPrice;

                await this.browser.yaCheckLink2({
                    selector: wizard.showcase.firstItem1DescrItemLink(),
                    url: {
                        href: `${origin}/moskva/cars/kia/rio/new/`,
                        ignore,
                    },
                    baobab: {
                        path: '/$page/$main/$result/showcase/item/subtitle',
                    },
                });
            });

            it('Третья тумба витрины', async function() {
                const PO = this.PO;
                const wizard = PO.autoTypeThumbsPrice;

                await this.browser.yaCheckLink2({
                    selector: wizard.showcase.firstItemHint(),
                    url: {
                        href: `${origin}/moskva/cars/kia/rio/new/`,
                        ignore,
                    },
                    baobab: {
                        path: '/$page/$main/$result/showcase/item/text',
                    },
                });
            });

            it('Кнопка \'Больше объявлений\'', async function() {
                const PO = this.PO;
                const wizard = PO.autoTypeThumbsPrice;

                await this.browser.yaScrollContainer(wizard.showcase.scrollWrap(), 9999);

                await this.browser.yaAssertViewExtended('scrolled', wizard(), {
                    hideElements: [PO.popup2()],
                    horisontalOffset: 26,
                    verticalOffset: 0,
                });

                await this.browser.yaCheckLink2({
                    selector: wizard.showcase.moreThumbLink(),
                    url: {
                        href: `${origin}/moskva/cars/kia/rio/all/`,
                        ignore,
                    },
                    baobab: {
                        path: '/$page/$main/$result/showcase/more',
                    },
                });
            });
        });
    });

    describe('Вид без цен', function() {
        beforeEach(async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsText;

            await this.browser.yaOpenSerp({
                foreverdata: '1330522542',
                data_filter: 'autoru-thumbs-text',
            }, wizard());
        });

        it('Заголовок', async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsText;

            await this.browser.assertView('plain', wizard());

            await this.browser.yaCheckLink2({
                selector: wizard.title.link(),
                url: {
                    href: `${origin}/moskva/cars/bmw/all/`,
                    ignore,
                },
                baobab: {
                    path: '/$page/$main/$result/title',
                },
            });
        });

        it('Гринурл', async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsText;

            await this.browser.yaCheckLink2({
                selector: wizard.greenurl.link(),
                url: {
                    href: `${origin}/moskva/cars/bmw/all/`,
                    ignore,
                },
                baobab: {
                    path: '/$page/$main/$result/path/urlnav',
                },
            });
        });

        it('Ссылки в тумбах', async function() {
            const PO = this.PO;
            const wizard = PO.autoTypeThumbsText;

            await this.browser.yaCheckLink2({
                selector: wizard.showcase.firstItemThumbLink(),
                url: {
                    href: `${origin}/moskva/cars/bmw/5er/all/`,
                    ignore,
                },
                baobab: {
                    path: '/$page/$main/$result/showcase/item/thumb',
                },
            });

            await this.browser.yaCheckLink2({
                selector: wizard.showcase.firstItem1DescrItemLink(),
                url: {
                    href: `${origin}/moskva/cars/bmw/5er/all/`,
                    ignore,
                },
                baobab: {
                    path: '/$page/$main/$result/showcase/item/subtitle',
                },
            });

            await this.browser.yaCheckLink2({
                selector: wizard.showcase.firstItem2DescrItemLink(),
                url: {
                    href: `${origin}/moskva/cars/bmw/5er/all/`,
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
                    href: `${origin}/moskva/cars/bmw/all/`,
                    ignore,
                },
                baobab: {
                    path: '/$page/$main/$result/showcase/more',
                },
            });
        });
    });

    it('Многострочная витрина', async function() {
        const PO = this.PO;
        const wizard = PO.autoTypeThumbsPrice;

        await this.browser.yaOpenSerp({
            foreverdata: 2560760563,
            data_filter: 'no_results',
        }, wizard());

        await this.browser.assertView('multiline', wizard());

        await this.browser.setBaseMetrics(metrics => metrics.concat([
            'web.total_wiz-auto_2_count',
            'web.total_auto.ru_clicks',
        ]));
    });
});
