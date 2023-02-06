'use strict';

const PO = require('./Stateful.page-object')('touch-phone');

specs('Колдунщик stateful сценария', function() {
    describe('Активен первый таб', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '3566695860',
            }, PO.statefulSerpItem.stateful());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful', PO.statefulSerpItem());
        });

        it('Ссылка на заголовке', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.statefulSerpItem.stateful.titleLink(),
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/stateful/title',
                    data: { themeId: '4321' },
                },
            });
        });

        it('Ссылка на карточке', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.statefulSerpItem.stateful.secondButton(),
                ajax: false,
                baobab: {
                    path: '/$page/$main/$result/stateful/recs/scroller/link',
                    data: { themeId: '4321' },
                },
            });
        });

        it('Ссылка в футере', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.statefulSerpItem.stateful.moreLink(),
                target: '_blank',
                baobab: {
                    path: '/$page/$main/$result/stateful/more-link',
                    data: { themeId: '4321' },
                },
            });
        });

        it('Переключение таба', async function() {
            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulSerpItem.stateful.secondTab()),
                {
                    path: '/$page/$main/$result/stateful/tabs/scroller/tab',
                    behaviour: { type: 'dynamic' },
                    data: { themeId: '4321' },
                },
            );
            await this.browser.yaWaitForVisible(PO.statefulSerpItem.stateful.activeSecondTab());
            await this.browser.assertView('stateful-change-tab', PO.statefulSerpItem());
        });
    });

    describe('С посещенной кнопкой', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '3179424839',
            }, PO.statefulSerpItem.stateful());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-second-tab', PO.statefulSerpItem());
        });
    });

    describe('Редизайн важных вопросов', function() {
        hermione.also.in('iphone-dark');
        it('Внешний вид с разными кнопками', async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '3590043989',
            }, PO.statefulSerpItem.stateful());

            await this.browser.assertView('stateful-redesign', PO.statefulSerpItem());
        });

        it('Внешний вид шторки', async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '3590043989',
            }, PO.statefulSerpItem.stateful());

            await this.browser.click(PO.statefulSerpItem.stateful.title());

            await this.browser.yaWaitForVisible(PO.statefulDrawer());
            await this.browser.assertView('stateful-drawer-redesign', PO.statefulDrawer(), {
                hideElements: [PO.main(), PO.header()],
            });
        });
    });

    describe('Полная версия колдуна на странице сценария - с посещенной кнопкой', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '1980446186',
            }, PO.statefulSerpItem.stateful());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-complete-travel', PO.statefulSerpItem());
        });

        hermione.also.in('iphone-dark');
        it('Шторка', async function() {
            await this.browser.click(PO.statefulSerpItem.stateful.promoLink());
            await this.browser.yaWaitForVisible(PO.statefulDrawer());
            await this.browser.assertView('stateful-drawer', PO.statefulDrawer(), {
                hideElements: [PO.main(), PO.header()],
            });
        });
    });

    describe('Активен второй таб', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '3674600647',
            }, PO.statefulSerpItem.stateful());
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-second-tab', PO.statefulSerpItem());
        });
    });

    describe('Подскролл к активному табу вне экрана', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '3675505366',
            }, PO.statefulSerpItem.stateful());
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('scrolled-to-last-tab', PO.statefulSerpItem());
        });
    });

    describe('Подскролл к активной кнопке вне экрана', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '674043677',
            }, PO.statefulSerpItem.stateful());
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('scrolled-to-current-button', PO.statefulSerpItem());
        });
    });

    describe('Полная версия колдуна на странице сценария - Трэвел', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '3132771543',
            }, PO.statefulSerpItem.stateful());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-complete-travel', PO.statefulSerpItem());
        });
    });

    describe('Полная версия колдуна на странице сценария - Карьера', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '981421417',
            }, PO.statefulSerpItem.stateful());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-complete-career', PO.statefulSerpItem());
        });
    });

    describe('Полная версия колдуна на странице сценария', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                data_filter: 'stateful',
                foreverdata: '2542891927',
            }, PO.statefulSerpItem.stateful());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('stateful-complete', PO.statefulSerpItem());
        });

        it('Ссылка на заголовке', async function() {
            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulSerpItem.stateful.titleLink()),
                {
                    path: '/$page/$main/$result/stateful/title',
                    behaviour: { type: 'dynamic' },
                },
            );
        });

        it('Ссылка в промоблоке', async function() {
            await this.browser.yaCheckBaobabCounter(
                () => this.browser.click(PO.statefulSerpItem.stateful.promoLink()),
                {
                    path: '/$page/$main/$result/stateful/promo',
                    behaviour: { type: 'dynamic' },
                },
            );
        });

        describe('Шторка', function() {
            beforeEach(async function() {
                await this.browser.click(PO.statefulSerpItem.stateful.promoLink());
                await this.browser.yaWaitForVisible(PO.statefulDrawer());
            });

            hermione.also.in('iphone-dark');
            it('Внешний вид', async function() {
                await this.browser.assertView('stateful-drawer', PO.statefulDrawer(), {
                    hideElements: [PO.main(), PO.header()],
                });
            });

            it('Расхлоп и кнопка', async function() {
                await this.browser.yaWaitForVisible(PO.statefulDrawer.StatefulGroupFirst());

                await this.browser.yaShouldBeSame(
                    PO.statefulDrawer.StatefulGroupFirst(),
                    PO.statefulDrawer.CollapserOpened(),
                );

                await this.browser.yaCheckBaobabCounter(
                    () => this.browser.click(PO.statefulDrawer.StatefulGroupFirst.Label()),
                    {
                        path: '/$page/$main/$result/stateful/drawer/stateful-group/Collapser/link',
                        behaviour: { type: 'dynamic' },
                    },
                );

                await this.browser.yaWaitForHidden(PO.statefulDrawer.CollapserOpened());

                await this.browser.yaCheckBaobabCounter(
                    () => this.browser.click(PO.statefulDrawer.StatefulGroupLast.Label()),
                    {
                        path: '/$page/$main/$result/stateful/drawer/stateful-group/Collapser/link',
                        behaviour: { type: 'dynamic' },
                    },
                );

                await this.browser.yaWaitForVisible(PO.statefulDrawer.CollapserOpened());

                await this.browser.yaShouldBeSame(
                    PO.statefulDrawer.StatefulGroupLast(),
                    PO.statefulDrawer.CollapserOpened(),
                );

                await this.browser.assertView('stateful-drawer-with-button', PO.statefulDrawer(), {
                    hideElements: [PO.main(), PO.header()],
                });

                await this.browser.yaCheckBaobabCounter(
                    () => this.browser.click(PO.statefulDrawer.StatefulGroupLast.Button()),
                    {
                        path: '/$page/$main/$result/stateful/drawer/stateful-group/Collapser/show-all',
                        behaviour: { type: 'dynamic' },
                    },
                );

                await this.browser.yaWaitForHidden(PO.statefulDrawer.StatefulGroupLast.Button());

                await this.browser.assertView('stateful-drawer-after-button-click', PO.statefulDrawer(), {
                    hideElements: [PO.main(), PO.header()],
                });
            });

            it('Ссылка при клике на вопрос', async function() {
                await this.browser.yaCheckLink2({
                    selector: PO.statefulDrawer.StatefulGroupFirst.Link(),
                    ajax: false,
                    baobab: {
                        path: '/$page/$main/$result/stateful/drawer/stateful-group/Collapser/question/link',
                    },
                });
            });
        });
    });
});
