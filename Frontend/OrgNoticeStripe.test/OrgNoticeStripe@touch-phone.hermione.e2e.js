'use strict';

const PO = require('./OrgNoticeStripe.page-object')('touch-phone');

hermione.only.notIn('searchapp-phone');
specs({
    feature: 'Колдунщик 1орг',
    type: 'Новый признак для переехавших',
}, function() {
    it('Проверка статуса "Адрес изменился"', async function() {
        const fallbackUrl = [
            '/search/touch/?text=росаккредитация официальный сайт&lr=213',
            '/search/touch/?text=шаляпин отель москва&lr=213',
            '/search/touch/?text=деловой клуб эталон Почтамтская ул., 2/9, Санкт-Петербург&lr=213',
        ];

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="companies" and @subtype="company"]/composite/notice-stripe[@type="move/somewhere"]',
            PO.oneOrg.noticeStripe(),
            fallbackUrl,
            async () => {
                await this.browser.yaShouldBeVisible(PO.oneOrg.noticeStripe());
                await this.browser.yaHaveVisibleText(PO.oneOrg.noticeStripe(), /^Адрес изменился$/);
            },
        );
    });

    it('Проверка статуса "Больше не работает"', async function() {
        const fallbackUrl = [
            '/search/touch?text=прокондитер ру',
            '/search/touch?text=социализм русские ооо саратов',
            '/search/touch?text=теленэт байкальск слюдянка',
        ];

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="companies" and @subtype="company"]/composite/notice-stripe[@type="close/unlimited"]',
            PO.oneOrg.noticeStripe(),
            fallbackUrl,
            async () => {
                await this.browser.yaShouldBeVisible(PO.oneOrg.noticeStripe());
                await this.browser.yaHaveVisibleText(PO.oneOrg.noticeStripe(), /^Больше не работает$/);
            },
        );
    });

    it('Проверка статуса "Организация переехала" со ссылкой на новый', async function() {
        const fallbackUrl = '/search/touch?text=делимобиль ленинский проспект';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result/composite/notice-stripe[@type="move/to"]',
            PO.oneOrg.noticeStripe.link(),
            fallbackUrl,
            async () => {
                await this.browser.yaShouldBeVisible(PO.oneOrg.noticeStripe());
                await this.browser.yaHaveVisibleText(PO.oneOrg.noticeStripe(), /^Организация переехала$/);
            },
        );
    });
});
