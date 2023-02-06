'use strict';
const PO = require('./UniSearchMedicine.page-object/index@common');

const defaultParams = {
    text: 'терапевт москва',
    'test-id': '494857',
};

specs({
    feature: 'Универсальный колдунщик поиска врачей',
}, function() {
    hermione.only.in('chrome-desktop');
    it('Дозагрузка (e2e)', async function() {
        await this.browser.yaOpenSerp(defaultParams, PO.UniSearchMedicine());

        const { length: currentCount } = await this.browser.elements(PO.UniSearchMedicine.Content.List.Item());
        assert(currentCount > 0, 'Нет врачей при открытии страницы');

        await this.browser.click(PO.UniSearchMedicine.Footer.More());
        await this.browser.yaWaitUntil('Не загрузились следующие врачи', async () => {
            const { length: nextCount } = await this.browser.elements(PO.UniSearchMedicine.Content.List.Item());
            return nextCount > currentCount;
        }, 3000);
    });
});
