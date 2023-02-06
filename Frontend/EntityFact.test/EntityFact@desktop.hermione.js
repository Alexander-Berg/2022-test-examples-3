const PO = require('../EntityFact.page-object/');

specs({
    feature: 'Факт',
    type: 'Объектный',
}, () => {
    beforeEach(async function() {
        await this.browser.yaOpenSerp(
            { text: 'сколько лет мадонне', data_filter: 'entity-fact' },
            PO.EntityFact(),
        );
    });

    it('Ссылка в тумбе', async function() {
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

    it('Шторка обратной связи', async function() {
        const { browser } = this;

        await this.browser.yaOpenSerp({
            text: 'сколько лет мадонне',
            data_filter: 'entity-fact',
        }, PO.EntityFact());

        await browser.click(PO.EntityFact.fact.reportButton());

        await browser.waitForVisible(PO.feedbackDialog(), 1000);

        await browser.assertView('feedback-drawer', PO.feedbackDialog(), {
            hideElements: ['body > :not(.Modal_visible)'],
        });
    });
});
