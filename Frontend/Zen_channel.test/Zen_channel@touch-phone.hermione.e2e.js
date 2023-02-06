'use strict';

const PO = require('./Zen_channel.page-object');

specs({
    feature: 'Колдунщик Дзена',
    type: 'Аватар и мета в сниппете блоггера',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="zen" and @subtype="channel"]',
            PO.zenChannel(),
            '/search/touch?text=вилсаком в дзене',
        );
    });

    it('Присутствует на выдаче', async function() {
        await this.browser.yaShouldBeVisible(PO.zenChannel.title(), 'Нет тайтла');
        await this.browser.yaShouldBeVisible(PO.zenChannel.zenHeader(), 'Нет шапки');
    });
});
