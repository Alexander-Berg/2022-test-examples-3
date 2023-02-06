'use strict';

hermione.only.notIn('searchapp-phone');
specs({
    feature: 'Колдунщик времени',
}, function() {
    it('Время в конкретном городе', async function() {
        const PO = this.PO;

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="time"]/title',
            PO.times.link(),
            '/search/?text=сколько времени в новосибирске',
        );
    });
});
