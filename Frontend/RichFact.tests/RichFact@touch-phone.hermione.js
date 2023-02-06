'use strict';

const { hideDrawerHandle } = require('../../../../hermione/client-scripts/hide-drawer-handle_touch');
const PO = require('./RichFact.page-object');

specs('Расширенный факт', () => {
    it('Текст - картинка - список', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 1415652665, data_filter: false }, PO.richFact());

        await browser.assertView('plain', PO.richFact());

        await browser.yaCheckLink2({
            selector: PO.richFact.link(),
            baobab: { path: '/$page/$main/$result/rich_fact/link' },
            target: '_blank',
            message: 'Неправильная ссылка',
        });

        await browser.yaCheckLink2({
            selector: PO.richFact.ECThumb(),
            baobab: { path: '/$page/$main/$result/rich_fact/thumb' },
            target: '_blank',
            message: 'Неправильная ссылка в картинке',
        });
    });

    it('Заголовок - текст с инлайн картинкой - список', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 422557946, data_filter: false }, PO.richFact());

        await browser.assertView('plain', PO.richFact());
    });

    it('Список второго уровня', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 3664134757, data_filter: false }, PO.richFact());

        await browser.assertView('unfolded', PO.richFact());
    });

    it('Несколько абзацев', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 2161780024, data_filter: false }, PO.richFact());

        await browser.yaCheckBaobabCounter(PO.richFact.fold.more(), {
            path: '/$page/$main/$result/rich_fact/fold/arrow[@behaviour@type="dynamic"]',
        });

        await browser.yaWaitForVisible(PO.richFact.foldUnfolded());

        await browser.assertView('unfolded', PO.richFact());

        await browser.yaCheckLink2({
            selector: PO.richFact.link(),
            baobab: { path: '/$page/$main/$result/rich_fact/fold/link' },
            target: '_blank',
            message: 'Неправильная ссылка',
        });
    });

    it('Список с числами', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'как найти потерянные airpods', data_filter: false }, PO.richFact());

        await browser.assertView('plain', PO.richFact());
    });

    it('Суммарный ответ и скрывающиеся элементы', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 1126770084, data_filter: false, srcskip: 'YABS_DISTR' }, PO.richFact());

        await browser.assertView('plain', PO.richFact());

        await browser.yaCheckBaobabCounter(PO.richFact.fold.more(), {
            path: '/$page/$main/$result/rich_fact/fold/arrow[@behaviour@type="dynamic"]',
        });

        await browser.assertView('unfolded', PO.richFact());

        await browser.yaCheckBaobabCounter(PO.richFact.collapserLabel(),
            { path: '/$page/$main/$result/rich_fact/fold/Collapser/open' },
        );

        await browser.assertView('uncollapsed', PO.richFact());
    });

    hermione.only.notIn('searchapp-phone', 'перезапрос на серч-аппе работает странно');
    it('Со связанными фактами', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: '424076375', data_filter: false }, PO.richFact());

        await browser.assertView('plain', PO.richFact());

        await this.browser.yaWaitUntilSerpReloaded(async () => {
            const dataset = await this.browser.yaCheckBaobabCounter(PO.richFact.relatedButton(), {
                path: '/$page/$main/$result/rich_fact/related/link',
            });

            return this.browser.yaCheckURL(dataset[0].raw.url, {
                pathname: '/search/touch/',
                queryValidator: query => query.text === 'ленинградский вокзал станция метро',
            }, 'Ссылка на кнопке должна перезапрашивать серп', { skipProtocol: true, skipHostname: true });
        });
    });

    hermione.also.in('iphone-dark');
    it('Кнопка телефона отображается в Summary', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: '2367170146', data_filter: false, srcskip: 'YABS_DISTR' }, PO.richFact());

        await browser.assertView('plain', PO.richFact());
    });

    it('Флаг для раскрытия коллапсеров', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 14695642, exp_flags: ['fact_rich_fact_expand_collapsers=1'], data_filter: false }, PO.richFact());

        await browser.yaCheckBaobabCounter(PO.richFact.fold.more(), {
            path: '/$page/$main/$result/rich_fact/fold/arrow[@behaviour@type="dynamic"]',
        });

        await browser.yaShouldBeVisible(PO.richFact.collapserOpened(), 'Коллапсер не раскрылся');
    });

    it('С меткой «Подтверждено» (recommendation_label)', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'как получить снилс',
            data_filter: 'rich_fact',
        }, PO.richFact());

        await browser.assertView('fact-source', PO.richFact.source());

        await browser.click(PO.richFact.verified());

        await browser.execute(function(selector) {
            document.querySelector(selector).scrollIntoView();
        }, PO.richFactVerifiedTooltip());

        await browser.click(PO.richFact.verified());

        await browser.yaWaitForVisible(PO.richFactVerifiedTooltip());

        await browser.assertView('tooltip', PO.richFactVerifiedTooltip());
    });

    it('С меткой «Подтверждено» (cbrf_info)', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 593328920, data_filter: 'rich_fact' }, PO.richFact());

        await browser.assertView('plain', PO.richFact());

        await browser.click(PO.richFact.verified());

        await browser.execute(function(selector) {
            document.querySelector(selector).scrollIntoView();
        }, PO.richFactVerifiedTooltip());

        await browser.click(PO.richFact.verified());

        await browser.yaWaitForVisible(PO.richFactVerifiedTooltip());

        await browser.assertView('tooltip', PO.richFactVerifiedTooltip(), {
            captureElementFromTop: false,
            allowViewportOverflow: true,
        });
    });

    hermione.also.in('iphone-dark');
    it('Факт с социальным источником', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'москва',
            foreverdata: 3361919896,
        }, PO.richFact());

        await browser.yaScroll(700);

        await browser.assertView('plain', PO.richFact());
    });

    hermione.only.notIn(['searchapp-phone'], 'Не работает подскролл контента попапа');
    it('Шторка обратной связи', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'как сделать скриншот на маке',
            data_filter: 'rich_fact',
        }, PO.richFact());

        await browser.click(PO.richFact.reportButton());

        await browser.waitForVisible(PO.feedbackDialog(), 1000);

        await browser.execute(hideDrawerHandle);

        await browser.assertView('feedback-drawer', PO.feedbackDialog(), {
            selectorToScroll: '.Drawer-Content',
            hideElements: ['body > :not(.Popup2_visible)'],
        });
    });

    hermione.also.in('iphone-dark');
    it('Фильтры в екоме', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'как выбрать монитор',
            foreverdata: 2878771781,
            data_filter: 'rich_fact',
        }, PO.ecom());

        await browser.assertView('plain', PO.ecom.FilterSelectorButton());

        await browser.yaCheckBaobabCounter(PO.ecom.FilterSelectorButton(), {
            path: '/$page/$main/$result/rich_fact/filter-selector-button',
        });

        await browser.pause(250);

        await browser.yaWaitForVisible(PO.FilterDrawer());

        await browser.assertView('drawer', PO.FilterDrawer.content());

        await browser.yaCheckBaobabCounter(PO.FilterDrawer.firstFilter.Label(), {
            path: '/$page/$main/$result/rich_fact/filter-drawer-checkbox',
            attrs: { filterId: '4913586', valueId: '12103913' },
        }, 's: ' + PO.FilterDrawer.firstFilter());

        await browser.yaCheckBaobabCounter(PO.FilterDrawer.More(), {
            path: '/$page/$main/$result/rich_fact/filter-drawer-more',
        });

        await browser.assertView('drawer2', PO.FilterDrawer.content());

        await this.browser.yaCheckLink2({
            selector: PO.FilterDrawer.submit(),
            url: {
                href: 'https://yandex.ru/products/search?text=%D0%BC%D0%BE%D0%BD%D0%B8%D1%82%D0%BE%D1%80%D1%8B&hid=91052&glfilter=4913586:12103913',
                ignore: ['hostname'],
            },
            baobab: {
                path: '/$page/$main/$result/rich_fact/filter-drawer-submit',
            },
        });
    });
});
