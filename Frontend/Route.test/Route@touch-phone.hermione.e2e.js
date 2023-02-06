'use strict';

const PO = require('./Route.page-object');

specs({
    feature: 'Колдунщик маршрутов',
}, function() {
    it('Проверка наличия элементов', async function() {
        const fallbackUrl = '/search/touch?text=построить маршрут';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="route"]/title',
            PO.route(),
            fallbackUrl,
        );
        await this.browser.yaShouldBeVisible(PO.route.organic.title(), 'Нет тайтла');
        await this.browser.yaShouldBeVisible(PO.route.organic.text(), 'Нет текста сниппета');
        await this.browser.yaShouldBeVisible(PO.route.goAuto(), 'Нет кнопки "На машине"');
        await this.browser.yaShouldBeVisible(PO.route.goMt(), 'Нет кнопки "Транспортом"');
    });
});
