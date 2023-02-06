const adminPage = require('../../../hermione/pages/admin');
const { gotoOrganization } = require('./helpers/common');
const { assert } = require('chai');

hermione.only.in('chrome-desktop');

describe('Домены', function () {
    describe('Домены - список', function () {
        it('diskforbusiness-523: Отображение плашки о некорректной МХ-записи для основного домена', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

            await gotoOrganization(bro, true);

            await bro.url('/domains');

            await bro.yaWaitForVisible(
                adminPage.alertNotification(),
                'Не отобразилась плашка'
            );
            await bro.assertView(
                'alert-notification',
                adminPage.alertNotification()
            );
        });
        it('diskforbusiness-506: Переход в саппорт по кнопке "Как исправить" из плашки о некорректной МХ-записи', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

            await gotoOrganization(bro, true);

            await bro.url('/domains');
            await bro.yaWaitForVisible(
                adminPage.alertNotification(),
                'Не отобразилась плашка'
            );
            await bro.click(adminPage.alertNotification.action());
            const tabs = await bro.getTabIds();
            await bro.switchTab(tabs[1]);
            const currentUrl = await bro.getUrl();

            assert(
                currentUrl.startsWith('https://yandex.ru/support/business'),
                'Нет перехода в справку'
            );
        });
        it('diskforbusiness-504: Некорректная MX-запись в списке доменов', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

            await gotoOrganization(bro, true);

            await bro.url('/domains');
            await bro.yaWaitForVisible(
                adminPage.domainWithText('Yndx-sarah-test-018.adm-testliza.ru'),
                'Домен не отобразился'
            );
            await bro.yaAssertView(
                'incorrect-domain',
                adminPage.domainWithText('Yndx-sarah-test-018.adm-testliza.ru')
            );
        });
        it('diskforbusiness-505: Отсутствует МХ-запись в списке доменов', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

            await gotoOrganization(bro, true);

            await bro.url('/domains');
            await bro.yaWaitForVisible(
                adminPage.domainWithText('Yndx-sarah-test-018.adm-testliza.ru'),
                'Домен не отобразился'
            );
            await bro.yaAssertView(
                'incorrect-domain',
                adminPage.domainWithText(
                    'Yndx-sarah-test-018-no-mx.adm-testliza.ru'
                )
            );
        });
    });
    describe('Домены - страница домена', function () {
        it('diskforbusiness-503: Отображение плашки о некорректной MX-записи', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

            await gotoOrganization(bro, true);

            await bro.url(
                '/domains/yndx-sarah-test-018.adm-testliza.ru?action=mx_set'
            );

            await bro.yaWaitForVisible(
                adminPage.alertNotification(),
                'Не отобразилась плашка'
            );
            await bro.assertView(
                'alert-notification',
                adminPage.alertNotification()
            );
        });
        it('diskforbusiness-506: Переход в саппорт по кнопке "Как исправить" из плашки о некорректной МХ-записи', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

            await gotoOrganization(bro, true);

            await bro.url(
                '/domains/yndx-sarah-test-018.adm-testliza.ru?action=mx_set'
            );
            await bro.yaWaitForVisible(
                adminPage.alertNotification(),
                'Не отобразилась плашка'
            );
            await bro.click(adminPage.alertNotification.action());
            const tabs = await bro.getTabIds();
            await bro.switchTab(tabs[1]);
            const currentUrl = await bro.getUrl();

            assert(
                currentUrl.startsWith('https://yandex.ru/support/business'),
                'Нет перехода в справку'
            );
        });
        it('diskforbusiness-527: Переход в саппорт из страницы проверки статуса MX-записи', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

            await gotoOrganization(bro, true);

            await bro.url(
                '/domains/yndx-sarah-test-018.adm-testliza.ru?action=mx_set'
            );
            await bro.yaWaitForVisible(
                adminPage.linkByText('Как это сделать'),
                'Не отобразилась ссылка'
            );
            await bro.click(adminPage.linkByText('Как это сделать'));
            const tabs = await bro.getTabIds();
            await bro.switchTab(tabs[1]);
            const currentUrl = await bro.getUrl();

            assert(
                currentUrl.startsWith('https://yandex.ru/support/business'),
                'Нет перехода в справку'
            );
        });
        it('diskforbusiness-524: Проверка статуса при отсутствующей МХ-записи', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

            await gotoOrganization(bro, true);

            await bro.url(
                '/domains/yndx-sarah-test-018-no-mx.adm-testliza.ru?action=mx_set'
            );
            await bro.yaWaitForVisible(
                adminPage.domainNoMxStatus(),
                'Не отобразилась ссылка'
            );
            await bro.yaAssertView('invalidMX', adminPage.domainNoMxStatus());
        });
        it('diskforbusiness-526: Проверка статуса при некорректной МХ-записи', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-018', 'gfhjkm13-gfhjkm13');

            await gotoOrganization(bro, true);

            await bro.url(
                '/domains/yndx-sarah-test-018.adm-testliza.ru?action=mx_set'
            );
            await bro.yaWaitForVisible(
                adminPage.domainInvalidMxStatus(),
                'Не отобразилась ссылка'
            );
            await bro.yaAssertView('noMX', adminPage.domainInvalidMxStatus());
        });
    });
});
