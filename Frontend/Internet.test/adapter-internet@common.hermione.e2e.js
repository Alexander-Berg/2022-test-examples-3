'use strict';

hermione.only.notIn('searchapp-phone');
specs({
    feature: 'Факт',
    type: 'Интернет',
}, function() {
    it('Проверка показа блоков', async function() {
        const PO = this.PO;

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="my_ip"]',
            PO.internetFact(),
            '/search/?text=мой ip',
        );
    });
});
