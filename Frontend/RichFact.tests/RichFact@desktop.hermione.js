'use strict';

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

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 3998832003, data_filter: false }, PO.richFact());

        await browser.assertView('unfolded', PO.richFact());

        await browser.yaCheckLink2({
            selector: PO.richFact.link(),
            baobab: { path: '/$page/$main/$result/rich_fact/link' },
            target: '_blank',
            message: 'Неправильная ссылка',
        });
    });

    it('Список с числами', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 2056998479, data_filter: false }, PO.richFact());

        await browser.assertView('plain', PO.richFact());
    });

    it('Суммарный ответ и скрывающиеся элементы', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 1126770084, data_filter: false }, PO.richFact());

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

    it('Суммарный ответ', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 3656539762, data_filter: false }, PO.richFact());

        await browser.assertView('plain', PO.richFact());
    });

    it('Флаг для раскрытия коллапсеров', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 2996317234, exp_flags: ['fact_rich_fact_expand_collapsers=1'], data_filter: false }, PO.richFact());

        await browser.yaCheckBaobabCounter(PO.richFact.fold.more(), {
            path: '/$page/$main/$result/rich_fact/fold/arrow[@behaviour@type="dynamic"]',
        });

        await browser.yaShouldBeVisible(PO.richFact.collapserOpened(), 'Коллапсер не раскрылся');
    });

    it('С меткой «Подтверждено» (recommendation_label)', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'как получить снилс',
            data_filter: false,
        }, PO.richFact());

        await browser.assertView('fact-source', PO.richFact.source());

        await browser.moveToObject(PO.richFact.verified());

        await browser.yaWaitForVisible(PO.richFactVerifiedTooltip());

        await browser.assertView('tooltip', PO.richFactVerifiedTooltip());
    });

    it('С меткой «Подтверждено» (cbrf_info)', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 593328920, data_filter: 'rich_fact' }, PO.richFact());

        await browser.assertView('plain', PO.richFact());

        await browser.moveToObject(PO.richFact.verified());

        await browser.yaWaitForVisible(PO.richFactVerifiedTooltip());

        await browser.assertView('tooltip', PO.richFactVerifiedTooltip());
    });

    it('Факт с социальным источником', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'москва',
            foreverdata: 3361919896,
        }, PO.richFact());

        await browser.assertView('plain', PO.richFact());
    });

    it('Со связанными фактами', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: '424076375', data_filter: false }, PO.richFact());

        await browser.assertView('plain', PO.richFact());

        await this.browser.yaWaitUntilSerpReloaded(async () => {
            const dataset = await this.browser.yaCheckBaobabCounter(PO.richFact.relatedButton(), {
                path: '/$page/$main/$result/rich_fact/related/link',
            });

            return this.browser.yaCheckURL(dataset[0].raw.url, {
                pathname: '/search/',
                queryValidator: query => query.text === 'ленинградский вокзал станция метро',
            }, 'Ссылка на кнопке должна перезапрашивать серп', { skipProtocol: true, skipHostname: true });
        });
    });

    it('Шторка обратной связи', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'как сделать скриншот на маке',
        }, PO.richFact());

        await browser.click(PO.richFact.reportButton());

        await browser.waitForVisible(PO.feedbackDialog(), 1000);

        await browser.assertView('feedback-drawer', PO.feedbackDialog(), {
            hideElements: ['body > :not(.Modal_visible)'],
        });
    });
});
