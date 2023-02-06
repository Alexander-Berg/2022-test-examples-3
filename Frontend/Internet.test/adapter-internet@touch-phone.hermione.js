'use strict';

specs({
    feature: 'Факт',
    type: 'Интернет (горизонтальный вид)',
}, () => {
    hermione.only.notIn(['iphone'], 'orientation is not supported');
    it('Загрузка адреса по ajax - горизонтальная ориентация', async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            text: 'мой ip',
            exp_flags: 'ip-address-ajax-test-mode',
            data_filter: false,
        }, PO.internetFact());

        await this.browser.setOrientation('landscape');

        await this.browser.yaWaitUntil('Не удалось получить адрес по ajax', async () => {
            const value = await this.browser.getValue(PO.internetFact.fact.ajaxIPAddress());
            return value !== '';
        });

        await this.browser.assertView('plain-horizontal', PO.internetFact());
    });
});
