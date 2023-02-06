'use strict';

specs({
    feature: 'Факт',
    type: 'Интернет',
}, () => {
    const serviceUrl = 'https://yandex.ru/internet';
    const path = '/$page/$main/$result';

    beforeEach(async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            text: 'мой ip',
            exp_flags: 'ip-address-ajax-test-mode',
            data_filter: false,
        }, PO.internetFact());
    });

    it('Загрузка адреса по ajax', async function() {
        const PO = this.PO;

        await this.browser.yaWaitUntil('Не удалось получить адрес по ajax', async () => {
            const value = await this.browser.getValue(PO.internetFact.fact.ajaxIPAddress());
            return value !== '';
        });

        await this.browser.assertView('plain', PO.internetFactView());
    });

    it('Ссылки в карточке', async function() {
        const PO = this.PO;

        await this.browser.yaCheckBaobabServerCounter({
            path: `${path}/link`,
        });

        await this.browser.yaCheckLink2({
            selector: PO.internetFact.fact.answer.link(),
            url: {
                href: serviceUrl,
            },
            baobab: {
                path: `${path}/link`,
            },
        });

        await this.browser.yaCheckBaobabServerCounter({
            path: `${path}/title`,
        });

        await this.browser.yaCheckLink2({
            selector: PO.internetFact.fact.sourceLink(),
            url: {
                href: serviceUrl,
            },
            baobab: {
                path: `${path}/title`,
            },
        });
    });
});
