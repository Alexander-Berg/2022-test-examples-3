const PO = require('../../EntityFact.page-object/');

specs({ feature: 'Факт', type: 'Объектный' }, () => {
    it('Кнопка позвонить', async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="entity-fact"]/phone-button',
            PO.EntityFact.fact(),
            [
                '/search?text=телефон университета витте',
                '/search?text=телефон музей космонавтики москва',
                '/search?text=телефон у музея история истории газа в нижний новгород',
            ],
        );

        await this.browser.yaShouldBeVisible(PO.EntityFact.fact.phoneButton(), 'Нет кнопки телефона');
    });
});
