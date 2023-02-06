'use strict';

const PO = require('./OrgNoticeStripe.page-object')('touch-phone');

specs({
    feature: 'Колдунщик 1орг',
    type: 'Новый признак для переехавших',
}, () => {
    it('Проверка статуса "Больше не работает"', async function() {
        await checkNoticeStripeText.call(this, '4117313303', /^Больше не работает$/, {
            path: '/$page/$main/$result/composite/notice-stripe[@type="close/unlimited"]',
        });
    });

    it('Проверка статуса "Временно не работает"', async function() {
        await checkNoticeStripeText.call(this, '1537543778', /^Временно не работает$/, {
            path: '/$page/$main/$result/composite/notice-stripe[@type="close/short"]',
        });
    });

    it('Проверка статуса "Возможно, не работает"', async function() {
        await checkNoticeStripeText.call(this, '3493815061', /^Возможно, не работает$/, {
            path: '/$page/$main/$result/composite/notice-stripe[@type="close/maybe"]',
        });
    });

    it('Проверка статуса "Адрес изменился"', async function() {
        await checkNoticeStripeText.call(this, '4121264134', /^Адрес изменился$/, {
            path: '/$page/$main/$result/composite/notice-stripe[@type="move/somewhere"]',
        });
    });

    it('Проверка статуса "Организация переехала" со ссылкой на новый', async function() {
        await checkNoticeStripeText.call(this, '4160373552', /^Организация переехала$/);

        await this.browser.yaCheckLink2({
            url: {
                href: 'https://yandex.ru/maps',
                ignore: ['protocol', 'pathname', 'query', 'hash'],
            },
            target: '_blank',
            baobab: {
                path: '/$page/$main/$result/composite/notice-stripe[@type="move/to"]/link',
            },
            selector: PO.oneOrg.noticeStripe.link(),
            message: 'Сломана ссылка на новый адрес на картах ' +
                PO.oneOrg.noticeStripe.link(),
        });

        const url = await this.browser.getAttribute(PO.oneOrg.noticeStripe.link(), 'href');
        assert.match(url, /oid=\d+/, 'Ссылка не содержит параметр oid');
    });

    hermione.also.in('iphone-dark');
    it('Вход по QR-коду', async function() {
        await this.browser.yaOpenSerp({
            text: 'кофейня на братиславской',
            oid: 'b:229842109311',
            data_filter: 'companies',
        }, PO.oneOrg());

        await this.browser.assertView('plain', PO.oneOrg.noticeStripe());
    });
});

async function checkNoticeStripeText(foreverdata, expectedText, counter) {
    await this.browser.yaOpenSerp({
        foreverdata: foreverdata,
        data_filter: 'companies',
    }, PO.oneOrg.noticeStripe());

    await this.browser.assertView('plain', PO.oneOrg.noticeStripe());
    await this.browser.yaCheckBaobabServerCounter(counter);

    return this.browser.yaHaveVisibleText(
        PO.oneOrg.noticeStripe(),
        expectedText,
        'Сломался текстовый статус организации',
    );
}
