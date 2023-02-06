'use strict';

const PO = require('./OrgPossibleOwner.page-object').desktop;

hermione.only.in('chrome-desktop', 'a11y проверяем только в одном браузере');
specs({
    feature: 'Одна организация',
    type: 'Вы владелец',
}, function() {
    it('Доступность', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: '',
            foreverdata: '2425475770',
            data_filter: 'companies',
        }, PO.oneOrg());

        const closeTitle = await browser.getAttribute(PO.oneOrg.possibleOwner.close(), 'title');

        assert.equal(closeTitle, 'Закрыть', 'Ошибка в аттрибуте title на кнопке закрытия');

        await browser.yaShouldBeVisible(PO.oneOrg.possibleOwner(), 'Нет блока владельца');
        await browser.click(PO.oneOrg.possibleOwner.title());
        await browser.yaKeyPress('TAB');
        await browser.yaAssertViewExtended('focus-close', PO.oneOrg.possibleOwner.close(), {
            verticalOffset: 10,
            horisontalOffset: 10,
        });
        await browser.yaKeyPress('TAB');
        await browser.yaAssertViewExtended('focus-yes', PO.oneOrg.possibleOwner.yes(), {
            verticalOffset: 10,
            horisontalOffset: 10,
        });
        await browser.yaKeyPress('TAB');
        await browser.yaAssertViewExtended('focus-no', PO.oneOrg.possibleOwner.no(), {
            verticalOffset: 10,
            horisontalOffset: 10,
        });
        await browser.yaKeyPress('TAB');
    });
});
