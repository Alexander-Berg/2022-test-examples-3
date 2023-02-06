'use strict';

const PO = require('./OrgAbout.page-object').touchPhone;

function hideZaloginPopup() {
    $('.zalogin-app').css('display', 'none');
}

specs({
    feature: 'Одна организация',
    type: 'Блок о компании',
}, function() {
    it('Наличие элементов в карточке в оверлее', async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
            data_filter: 'companies',
        }, PO.oneOrg());

        // Скрываем попап залогина чтобы кликнуть на организацию в списке
        await this.browser.execute(hideZaloginPopup);

        await this.browser.click(PO.oneOrg.tabsMenu.about());
        await this.browser.yaWaitForVisible(PO.overlayOneOrg(), 'Оверлей с карточкой организации не открылся');
        await this.browser.yaWaitForVisible(PO.overlayOneOrg.OrgAbout(), 'В оверлее с карточкой организации нет описания');
    });
});
