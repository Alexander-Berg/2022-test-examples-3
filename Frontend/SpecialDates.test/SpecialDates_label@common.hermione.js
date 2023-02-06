'use strict';

specs({
    feature: 'Сниппеты',
    type: 'Лейбл метки свежести и отступ',
}, function() {
    hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
    it('Проверка внешнего вида - с текстом', async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '497091340',
            user_time: '20190610',
            data_filter: 'special-dates' }, PO.serpList.snippetWithLabel());

        await this.browser.assertView('freshnessSnipetLabel', PO.serpList.snippetWithLabel());
    });
});
