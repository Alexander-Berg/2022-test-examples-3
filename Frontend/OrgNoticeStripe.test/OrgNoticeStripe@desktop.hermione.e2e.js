'use strict';

const PO = require('./OrgNoticeStripe.page-object')('desktop');

specs({
    feature: 'Колдунщик 1орг',
    type: 'Новый признак для переехавших',
}, function() {
    it('Проверка статуса "Адрес изменился"', async function() {
        const fallbackUrl = [
            '/search/?text=ао ск опора&lr=213',
            '/search/?text=мосспортобъект гбу&lr=213',
            '/search/?text=росаккредитация официальный сайт&lr=213',
        ];

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result[@wizard_name="companies" and @subtype="company"]/composite/tabs/about/notice-stripe[@type="move/somewhere"]',
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
            '/search?text=прокондитер ру',
            '/search?text=социализм русские ооо саратов',
            '/search?text=теленэт байкальск слюдянка',
        ];

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result[@wizard_name="companies" and @subtype="company"]/composite/tabs/about/notice-stripe[@type="close/unlimited"]',
            PO.oneOrg.noticeStripe(),
            fallbackUrl,
            async () => {
                await this.browser.yaShouldBeVisible(PO.oneOrg.noticeStripe());
                await this.browser.yaHaveVisibleText(PO.oneOrg.noticeStripe(), /^Больше не работает$/);
            },
        );
    });

    it('Проверка статуса "Организация переехала" со ссылкой на новый', async function() {
        const fallbackUrl = '/search?text=делимобиль ленинский проспект';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result[@wizard_name="companies" and @subtype="company"]/composite/tabs/about/notice-stripe[@type="move/to"]',
            PO.oneOrg.noticeStripe.link(),
            fallbackUrl,
            async () => {
                await this.browser.yaShouldBeVisible(PO.oneOrg.noticeStripe());
                await this.browser.yaHaveVisibleText(PO.oneOrg.noticeStripe(), /^Организация переехала$/);
            },
        );
    });
});
