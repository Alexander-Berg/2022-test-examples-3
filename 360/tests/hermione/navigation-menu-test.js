const common = require('../../../hermione/pages/common');
const admin = require('../../../hermione/pages/admin');
const { gotoOrganization } = require('./helpers/common');

hermione.only.in('chrome-desktop');
describe('Меню навигации', function () {
    describe('Партнёрка', function () {
        it('diskforbusiness-600: Партнер: копирование ссылки для клиента кнопкой +Новый клиент', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-2',
                'pass-yndx-sarah-partner-2'
            );
            await bro.assertView(
                'partner-organizarion-list-without-new-org-btn',
                admin.organizationSelectionContainer()
            );
            await gotoOrganization(bro, true);
            await bro.yaWaitForVisible(
                common.navigationMenu(),
                'Не отобразилась меню навигации'
            );
            await bro.yaWaitForVisible(
                common.navigationMenu.addClientButton(),
                'Не отобразилась кнопка регистрации нового клиента'
            );
            await bro.assertView(
                'partner-navigation-menu',
                common.navigationMenu()
            );
            await bro.click(common.navigationMenu.addClientButton());
            await bro.yaWaitForVisible(
                common.modal(),
                'Не отобразилось модальное окно с ссылкой на регистрацию'
            );
            await bro.assertView('partner-new-client-modal', common.modal(), {
                invisibleElements: [common.modal.copy()]
            });
            await bro.click(common.copyRegistrationLinkButton());
            await bro.yaWaitForVisible(
                common.popup(),
                'Не отобразился попап копирования текста'
            );
            await bro.assertView('partner-copy-text-popup', common.popup());
        });
        it('diskforbusiness-601: Партнер: в разделе "Профиль организации" нет кнопки смены владельца', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-2',
                'pass-yndx-sarah-partner-2'
            );
            await gotoOrganization(bro, true);
            await bro.url(`/company-profile`);
            await bro.yaWaitForVisible(
                common.organizationPage(),
                'Не отобразилась страница с реквизитами'
            );
            await bro.assertView(
                'diskforbusiness-601-1',
                common.organizationPage()
            );
        });
        it('diskforbusiness-602: Клиент: в разделе "Профиль организации" нет кнопки смены владельца', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-3',
                'pass-yndx-sarah-partner-3'
            );
            await gotoOrganization(bro, true);
            await bro.url(`/company-profile`);
            await bro.yaWaitForVisible(
                common.organizationPage(),
                'Не отобразилась страница с реквизитами'
            );
            await bro.assertView(
                'diskforbusiness-602-1',
                common.organizationPage()
            );
        });
        it('diskforbusiness-603: Партнер: в разделе "Реквизиты" нет ссылки на раздел "Оплата и тарифы"', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-2',
                'pass-yndx-sarah-partner-2'
            );
            await gotoOrganization(bro, true);
            await bro.url(`/products/requisites`);
            await bro.yaWaitForVisible(
                common.productsRequisites(),
                'Не отобразилась страница с реквизитами'
            );
            await bro.assertView(
                'diskforbusiness-603-1',
                common.productsRequisites(),
                {
                    hideElements: [common.productsRequisites.form()]
                }
            );
        });
        it('diskforbusiness-604: Клиент: в разделе "Реквизиты" нет ссылки на раздел "Оплата и тарифы"', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-3',
                'pass-yndx-sarah-partner-3'
            );
            await gotoOrganization(bro, true);
            await bro.url(`/products/requisites`);
            await bro.yaWaitForVisible(
                common.productsRequisites(),
                'Не отобразилась страница с реквизитами'
            );
            await bro.assertView(
                'diskforbusiness-604-1',
                common.productsRequisites(),
                {
                    hideElements: [common.productsRequisites.form()]
                }
            );
        });
    });
});
