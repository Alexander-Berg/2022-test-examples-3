'use strict';

const PO = require('./EntityFeedback.page-object');

specs({
    feature: 'Объектный ответ',
    type: 'Шторка жалоб',
}, function() {
    const fallbacks = [
        '/search/?text=мадонна',
        '/search/?text=интерстеллар',
        '/search/?text=нигерия',
        '/search/?text=московский кремль',
    ];

    it('Проверка жалобщика', async function() {
        const { browser } = this;

        await browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result[@wizard_name="entity_search"]/FeedbackFooter/abuse',
            [PO.entityFooter()],
            fallbacks,
        );

        await browser.yaShouldBeVisible(PO.entityFooter(), 'На странице нету футера');
        await browser.yaWaitForVisible(PO.entityFooter.abuseLink(), 'Ссылка "Сообщить об ошибке должна присутствовать"');
        await browser.click(PO.entityFooter.abuseLink());
        await browser.yaWaitForVisible(PO.feedbackDialog(), 'Жалобщик не открылся');
    });
});
