'use strict';

const PO = require('./OrgsList.page-object').touchPhone;

function hideZaloginPopup() {
    $('.zalogin-app').css('display', 'none');
}

specs({
    feature: 'Список организаций',
}, function() {
    beforeEach(async function() {
        const fallbackUrl = '/search/touch?text=кафе';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result/composite/title[@wizard_name="companies" and @subtype="map"]',
            PO.OrgsList(),
            fallbackUrl,
            { srcskip: 'YABS_DISTR' },
        );
    });

    it('Наличие элементов', async function() {
        await this.browser.yaShouldBeVisible(PO.OrgsList.FirstItem(), 'Нет ни одной организации в списке');
        await this.browser.yaShouldBeVisible(PO.OrgsList.more(), 'Нет кнопки "Посмотреть ещё"');
    });

    it('Открытие оверлея организации', async function() {
        // Скрываем попап залогина чтобы кликнуть на организацию в списке
        await this.browser.execute(hideZaloginPopup);

        await this.browser.click(PO.OrgsList.FirstItem.OverlayHandler());
        await this.browser.yaWaitForVisible(PO.overlayOneOrg(), 'Оверлей с карточкой организации не открылся');
    });

    it('Дозагрузка списка организаций', async function() {
        const itemsCountBefore = await this.browser.yaVisibleCount(PO.OrgsList.Item());

        // Скрываем попап залогина чтобы кликнуть на кнопку "Посмотреть ещё"
        await this.browser.execute(hideZaloginPopup);

        await this.browser.click(PO.OrgsList.more());

        // Ждём загрузку новых элементов
        await this.browser.yaWaitUntil('Новые организации не загрузились', async () => {
            const newItemsCountBefore = await this.browser.yaVisibleCount(PO.OrgsList.Item());
            return newItemsCountBefore > itemsCountBefore;
        });
    });
});
