'use strict';

const PO = require('../ImageFact.page-object');

specs({
    feature: 'Факт',
    type: 'Одной картинкой',
}, function() {
    it('Наличие элементов', async function() {
        const fallback = [
            '/search/touch/?text=таблица менделеева картинка',
        ];

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="image_fact"]',
            PO.imageFact(),
            fallback,
        );
        await this.browser.yaShouldBeVisible(PO.imageFact.fact.title(), 'Нет тайтла');
        await this.browser.yaShouldBeVisible(PO.imageFact.fact.path(), 'Нет гринурла');
        await this.browser.yaShouldBeVisible(PO.imageFact.fact.image(), 'Нет картинки');
    });
});
