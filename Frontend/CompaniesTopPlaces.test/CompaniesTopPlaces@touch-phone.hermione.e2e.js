'use strict';

const PO = require('./CompaniesTopPlaces.page-object')('touch-phone');

function hideZaloginPopup() {
    $('.zalogin-app').css('display', 'none');
}

specs({ feature: 'Список ресторанов Мишлен' }, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'рестораны мишлен',
            srcskip: 'YABS_DISTR',
        }, PO.companiesTopPlaces());
    });

    it('Наличие элементов', async function() {
        await this.browser.yaShouldBeVisible(PO.companiesTopPlaces.title(), 'Нет тайтла');
        await this.browser.yaShouldBeVisible(PO.companiesTopPlaces.subtitle(), 'Нет сабтайтла');
        await this.browser.yaShouldBeVisible(PO.companiesTopPlaces.help(), 'Нет кнопки рассказывающей про мишлен');
        await this.browser.yaShouldBeVisible(PO.companiesTopPlaces.Map(), 'Нет карты');
        await this.browser.yaShouldBeVisible(PO.companiesTopPlaces.List(), 'Нет списка организаций');
        await this.browser.yaShouldBeVisible(PO.companiesTopPlaces.List.more(), 'Нет кнопки "Смотреть ещё"');
    });

    it('Открытие оверлея организации', async function() {
        // Скрываем попап залогина чтобы кликнуть на организацию в списке
        await this.browser.execute(hideZaloginPopup);

        await this.browser.click(PO.companiesTopPlaces.List.FirstItem.OverlayHandler());
        await this.browser.yaWaitForVisible(PO.overlayOneOrg(), 'Оверлей с карточкой организации не открылся');
    });

    it('Дозагрузка списка', async function() {
        const itemsCountBefore = await this.browser.yaVisibleCount(PO.companiesTopPlaces.List.Item());

        // Скрываем попап залогина чтобы кликнуть на кнопку "Посмотреть ещё"
        await this.browser.execute(hideZaloginPopup);

        await this.browser.click(PO.companiesTopPlaces.List.more());

        // Ждём загрузку новых элементов
        await this.browser.yaWaitUntil('Новые организации не загрузились', async () => {
            const newItemsCountBefore = await this.browser.yaVisibleCount(PO.companiesTopPlaces.List.Item());
            return newItemsCountBefore > itemsCountBefore;
        });
    });
});
