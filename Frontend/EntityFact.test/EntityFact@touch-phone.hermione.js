const PO = require('../EntityFact.page-object/');
const { hideDrawerHandle } = require('../../../../hermione/client-scripts/hide-drawer-handle_touch');

specs({
    feature: 'Факт',
    type: 'Объектный',
}, () => {
    hermione.also.in('iphone-dark');
    it('Ссылка в тумбе', async function() {
        await this.browser.yaOpenSerp(
            { text: 'сколько лет мадонне', data_filter: false },
            PO.EntityFact(),
        );

        await this.browser.assertView('plain', PO.EntityFact());

        await this.browser.yaCheckLink2({
            selector: PO.EntityFact.fact.thumb(),
            target: '_self',
            url: {
                queryValidator: query => query.text === 'Мадонна певица',
                ignore: ['protocol', 'hostname', 'pathname'],
            },
            baobab: {
                path: '/$page/$main/$result/thumb',
            },
        });
    });

    it('Ссылка в первой хлебной крошке', async function() {
        await this.browser.yaOpenSerp(
            { text: 'сколько лет мадонне', data_filter: 'entity-fact' },
            PO.EntityFact(),
        );

        await this.browser.yaCheckLink2({
            selector: PO.EntityFact.fact.question.link(),
            target: '_self',
            url: {
                queryValidator: query => query.text === 'Мадонна певица',
                ignore: ['protocol', 'hostname', 'pathname'],
            },
            baobab: {
                path: '/$page/$main/$result/question',
            },
        });
    });

    it('Горизонтальная картинка', async function() {
        await this.browser.yaOpenSerp({ text: 'деньги в швеции', data_filter: false }, PO.EntityFact());
        await this.browser.assertView('horizontal', PO.EntityFact());
    });

    hermione.only.notIn(['iphone']);
    it('Подсказка "Что это"', async function() {
        const text = 'сколько лет мадонне';
        // eslint-disable-next-line camelcase
        const data_filter = false;

        await this.browser.yaOpenSerp({ text, data_filter }, PO.EntityFact());
        await this.browser.yaCheckBaobabCounter(
            PO.factHeaderHint(),
            { path: '/$page/$main/fact-header/hint' },
        );
        await this.browser.assertView('drawer', PO.drawer(), {
            hideElements: [PO.header(), PO.main()],
        });
    });

    hermione.only.notIn(['searchapp-phone'], 'Не работает подскролл контента попапа');
    it('Шторка обратной связи', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'сколько лет мадонне',
            data_filter: 'entity-fact',
        }, PO.EntityFact());

        await browser.click(PO.EntityFact.fact.reportButton());

        await browser.waitForVisible(PO.feedbackDialog(), 1000);

        await browser.execute(hideDrawerHandle);

        await browser.assertView('feedback-drawer', '.FeedbackDialog', {
            selectorToScroll: '.Drawer-Content',
            hideElements: ['body > :not(.Popup2_visible)'],
        });
    });
});
