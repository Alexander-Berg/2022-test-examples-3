const page = require('../../../hermione/pages/master-registration');
const common = require('../../../hermione/pages/common');
const admin = require('../../../hermione/pages/admin');

const {
    gotoMasterRegistration,
    gotoPaymentStubPage
} = require('./helpers/master-registration');
const { gotoOrganization } = require('./helpers/common');
const { passportHost } = require('../../../hermione/constants');
const assert = require('chai').assert;

const PASSPORT_TAB_ID = 'ya_passport_child_window';

const VALID_INN = '7710140679';
const INVALID_INN = '1234567894';

hermione.only.in('chrome-desktop');
describe('Мастер регистрации', function () {
    describe('Обычный флоу', function () {
        it('diskforbusiness-416: Переключение пользователя на форме регистрации', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-021',
                'pass-yndx-sarah-test-021'
            );
            await bro.yaLoginFast(
                'yndx-sarah-test-022',
                'pass-yndx-sarah-test-022'
            );
            await gotoMasterRegistration(bro);
            await bro.assertView('diskforbusiness-416-1', 'body');
            await bro.click(page.chooseOwnerStep.select());
            await bro.click(
                page.chooseOwnerStep.selectUserByText('yndx-sarah-test-021')
            );
            await gotoMasterRegistration(bro);
            await bro.assertView('diskforbusiness-416-2', 'body');
        });

        it('diskforbusiness-417: Авторизация пдд-пользователем', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'domen@yug.auto.connect-tk.tk',
                'pass-yndx-sarah-test-022'
            );
            await gotoMasterRegistration(bro);
            await bro.assertView('diskforbusiness-417', page.chooseOwnerStep());
        });

        it('diskforbusiness-418: Автоподтягивание реквизитов на форму регистрации', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-023',
                'pass-yndx-sarah-test-023'
            );
            await gotoMasterRegistration(bro);
            await bro.click(page.chooseOwnerStep.continue());
            await bro.click(page.leftColumnStep(0));
            await bro.click(page.chooseOwnerStep.continue());
            await bro.setValue(
                page.setRequisitesStep.innSearchInput(),
                VALID_INN
            );
            await bro.yaWaitForVisible(
                page.setRequisitesStep.expandRequisites(),
                'Не подтянулись реквизиты'
            );
            await bro.click(page.setRequisitesStep.expandRequisites());
            await bro.assertView(
                'diskforbusiness-418',
                page.setRequisitesStep()
            );
        });

        it('diskforbusiness-419: Ввод несуществующего ИНН', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-023',
                'pass-yndx-sarah-test-023'
            );
            await gotoMasterRegistration(bro);
            await bro.click(page.chooseOwnerStep.continue());
            await bro.setValue(
                page.setRequisitesStep.innSearchInput(),
                INVALID_INN
            );
            await bro.yaWaitForVisible(
                page.setRequisitesStep.manualSetRequisites(),
                'Не появилась кнопка ручного ввода реквизитов'
            );
            await bro.assertView(
                'diskforbusiness-419-1',
                page.setRequisitesStep()
            );

            await bro.click(page.setRequisitesStep.manualSetRequisites());
            await bro.$(page.setRequisitesStep()).moveTo(0, 0);
            await bro.assertView(
                'diskforbusiness-419-2',
                page.setRequisitesStep()
            );
            await bro.url(`/version`);
        });

        hermione.skip.notIn(
            '',
            'Баланс в престейбле чаще выдает ошибки, чем нормальные ответы'
        );
        it('diskforbusiness-420: Выбор тарифа на шаге оплаты (оплата картой)', async function () {
            const bro = this.browser;
            // юзер с заглушкой на 3 шаге
            await bro.yaLoginFast(
                'yndx-sarah-test-024',
                'pass-yndx-sarah-test-024'
            );
            await gotoOrganization(bro);

            await bro.yaWaitForVisible(
                page.paymentStep(),
                'Не появился шаг оплаты'
            );
            await bro.yaWaitForHidden(
                page.paymentStep.iframeLoader(),
                'Лоадер айфрема не исчез',
                25000
            );
            await bro.assertView('diskforbusiness-420', page.paymentStep(), {
                hideElements: ['.checkout__main']
            });
        });

        it('diskforbusiness-421: Выбор тарифа на шаге оплаты (оплата по счету)', async function () {
            const bro = this.browser;
            await gotoPaymentStubPage(bro);
            await bro.click(page.paymentStep.payByBillTab());
            await bro.assertView('diskforbusiness-421-1', page.paymentStep());
            await bro.click(page.paymentStep.selectProduct());
            await bro.click(page.paymentStep.selectProductOption(1));
            await bro.assertView('diskforbusiness-421-2', page.paymentStep());
        });

        it('diskforbusiness-423: Выбор количества пользователей/месяцев на форме оплаты (оплата по счету)', async function () {
            const bro = this.browser;
            await gotoPaymentStubPage(bro);
            await bro.click(page.paymentStep.payByBillTab());
            await bro.assertView('diskforbusiness-423-1', page.paymentStep());
            await bro.click(page.paymentStep.monthCount());
            await bro.click(page.paymentStep.monthCount.plus());
            await bro.click(page.paymentStep());
            await bro.assertView('diskforbusiness-423-2', page.paymentStep());
            await bro.click(page.paymentStep.peopleCount());
            await bro.click(page.paymentStep.peopleCount.plus());
            await bro.click(page.paymentStep());
            await bro.assertView('diskforbusiness-423-3', page.paymentStep());
        });

        it('diskforbusiness-424: Сохранение шага с формой оплаты', async function () {
            const bro = this.browser;
            await gotoPaymentStubPage(bro);
            await bro.click(page.paymentStep.payByBillTab());
            await bro.assertView('diskforbusiness-424', page.paymentStep());
        });

        it('diskforbusiness-427: Создание 2+ платной организации с существующей платной орг-ей', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-025',
                'pass-yndx-sarah-test-025'
            );
            await gotoMasterRegistration(bro);
            await bro.assertView('diskforbusiness-427', 'body');
        });
        it('diskforbusiness-428: Создание 2+ бесплатной организации с существующей платной орг-ей', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-025',
                'pass-yndx-sarah-test-025'
            );
            await gotoMasterRegistration(bro, { free: true });
            await bro.assertView('diskforbusiness-428', 'body');
        });

        it('diskforbusiness-529: Открывается всплывающее окно при авторизации пользователя', async function () {
            const bro = this.browser;
            await gotoMasterRegistration(bro);
            await bro.click(page.anonimWrapper.signinButton());
            await bro.switchTab(PASSPORT_TAB_ID);
            let currentUrl = await bro.getUrl();
            assert.include(currentUrl, passportHost + '/auth');
        });

        it('diskforbusiness-532: Открывается окно создания пользователя', async function () {
            const bro = this.browser;
            await gotoMasterRegistration(bro);
            await bro.click(page.anonimWrapper.registerButton());
            await bro.switchTab(PASSPORT_TAB_ID);
            let currentUrl = await bro.getUrl();
            assert.include(currentUrl, passportHost + '/registration');
        });

        it('diskforbusiness-531: Закрыть окно авторизации', async function () {
            const bro = this.browser;
            await gotoMasterRegistration(bro);
            let currentTabId = await bro.getCurrentTabId();
            await bro.click(page.anonimWrapper.signinButton());
            await bro.switchTab(PASSPORT_TAB_ID);
            await bro.close();
            await bro.switchTab(currentTabId);
        });

        it('diskforbusiness-536: Закрыть окно создания пользователя', async function () {
            const bro = this.browser;
            await gotoMasterRegistration(bro);
            let currentTabId = await bro.getCurrentTabId();
            await bro.click(page.anonimWrapper.registerButton());
            await bro.switchTab(PASSPORT_TAB_ID);
            await bro.close();
            await bro.switchTab(currentTabId);
        });

        it('Регистрация 2+ организации с реквизитами с положительным балансом', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-035',
                'pass-yndx-sarah-test-035'
            );
            await gotoMasterRegistration(bro);

            await bro.assertView('second-org-positive-balance', page.root(), {
                hideElements: ['.Popup2']
            });
        });

        it('Регистрация 2+ организации с реквизитами с нулевым балансом', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-036',
                'pass-yndx-sarah-test-036'
            );
            await gotoMasterRegistration(bro);

            await bro.assertView('second-org-zero-balance', page.root(), {
                hideElements: ['.Popup2']
            });
        });
    });

    describe('Флоу образования', function () {
        it('diskforbusiness-511: Попытка регистрации образовательной организации', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-team-edu-0', 'testiwan');
            await gotoMasterRegistration(bro, { edu: true, soft: true });
            await bro.yaWaitForVisible(
                page.educationNotAvailable(),
                'Не отобразилась заглушка о недоступности регистрации образовательных организаций'
            );
            await bro.assertView('diskforbusiness-511', 'body');
        });
        it('diskforbusiness-512: Переход к списку организаций из заглушки образовательной организации', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-team-edu-0', 'testiwan');
            await gotoMasterRegistration(bro, { edu: true, soft: true });
            await bro.yaWaitForVisible(
                page.educationNotAvailable(),
                'Не отобразилась заглушка о недоступности регистрации образовательных организаций'
            );
            await bro.click(page.educationNotAvailable.toOrgsButton());
            await bro.yaWaitForVisible(
                page.selectOrgButton(0),
                'Не отобразилась кнопка выбора организации'
            );
            await bro.assertView('diskforbusiness-511', 'body');
        });

        it('diskforbusiness-513: Регистрация образовательной организации с ручным вводом реквизитов', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-026',
                'pass-yndx-sarah-test-026'
            );
            await gotoMasterRegistration(bro, { edu: true });
            await bro.click(page.chooseOwnerStep.continue());
            await bro.setValue(
                page.setRequisitesStep.innSearchInput(),
                INVALID_INN
            );
            await bro.yaWaitForVisible(
                page.setRequisitesStep.manualSetRequisites(),
                'Не появилась кнопка ручного ввода реквизитов'
            );
            await bro.assertView(
                'diskforbusiness-513-1',
                page.setRequisitesStep()
            );

            await bro.click(page.setRequisitesStep.manualSetRequisites());
            await bro.$(page.setRequisitesStep()).moveTo(0, 0);
            await bro.assertView(
                'diskforbusiness-513-2',
                page.setRequisitesStep()
            );
            await bro.url(`/version`);
        });

        it('diskforbusiness-514: Регистрация образовательной организации с автозаполнением реквизитов', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-026',
                'pass-yndx-sarah-test-026'
            );
            await gotoMasterRegistration(bro, { edu: true });
            await bro.click(page.chooseOwnerStep.continue());
            await bro.click(page.leftColumnStep(0));
            await bro.click(page.chooseOwnerStep.continue());
            await bro.setValue(
                page.setRequisitesStep.innSearchInput(),
                VALID_INN
            );
            await bro.yaWaitForVisible(
                page.setRequisitesStep.expandRequisites(),
                'Не подтянулись реквизиты'
            );

            await bro.click(page.setRequisitesStep.expandRequisites());
            await bro.assertView(
                'diskforbusiness-514',
                page.setRequisitesStep()
            );
        });

        it('diskforbusiness-515: Отображение баннера про ограниченную версию', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-025',
                'pass-yndx-sarah-test-025'
            );
            await gotoOrganization(bro, false, 0, [
                { key: 'success_education_registration', value: '1' }
            ]);

            await bro.assertView('diskforbusiness-515', 'body', {
                hideElements: ['.Popup2']
            });
        });

        it('diskforbusiness-516: Отображение попапа доступности образовательных тарифов', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-team-edu-2',
                '86d759a451115de7f4edb7ff26dca49d4a263fcc35622eca128d248d7b0dbe76'
            );
            await gotoOrganization(bro);

            await bro.assertView('diskforbusiness-516', 'body', {
                hideElements: ['.Popup2']
            });
        });

        it('Заглушка подтверждения реквизитов для образовательной организации', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                `yndx-sarah-test-037`,
                `pass-yndx-sarah-test-037`
            );
            await gotoOrganization(bro);
            await bro.yaWaitForVisible(
                page.root(),
                'Не появился мастер регистрации'
            );
            await bro.assertView('education-payment-stub', page.root());
        });
    });

    describe('Физики', function () {
        it('Заглушка оплаты для физика', async function () {
            const bro = this.browser;
            await gotoPaymentStubPage(bro, '032'); // yndx-sarah-test-032
            await bro.assertView('ph-payment-stub', 'body', {
                ignoreElements: [page.paymentStep.iframeLoader()]
            });
        });

        it('Регистрация 2ой организации для физика', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-032',
                'pass-yndx-sarah-test-032'
            );
            await gotoMasterRegistration(bro);
            await bro.assertView('ph-second-org', page.root());
        });
    });

    describe('Партнёрка', function () {
        it('diskforbusiness-592: Регистрация организации партнера с ручным вводом реквизитов', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-1',
                'pass-yndx-sarah-partner-1'
            );
            await gotoMasterRegistration(bro, { partnerReg: true });
            await bro.assertView('diskforbusiness-592-1', page.root());
            await bro.click(page.chooseOwnerStep.continue());
            await bro.assertView('diskforbusiness-592-2', page.root());

            await bro.setValue(
                page.setRequisitesStep.innSearchInput(),
                INVALID_INN
            );
            await bro.yaWaitForVisible(
                page.setRequisitesStep.manualSetRequisites(),
                'Не появилась кнопка ручного ввода реквизитов'
            );
            await bro.assertView(
                'diskforbusiness-592-3',
                page.setRequisitesStep()
            );

            await bro.click(page.setRequisitesStep.manualSetRequisites());
            await bro.$(page.setRequisitesStep()).moveTo(0, 0);
            await bro.assertView(
                'diskforbusiness-592-4',
                page.setRequisitesStep()
            );
        });
        it('diskforbusiness-593: Регистрация организации партнера с автозаполнением реквизитов', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-1',
                'pass-yndx-sarah-partner-1'
            );
            await gotoMasterRegistration(bro, { partnerReg: true });
            await bro.assertView('diskforbusiness-593-1', page.root());
            await bro.click(page.chooseOwnerStep.continue());
            await bro.assertView('diskforbusiness-593-2', page.root());
            await bro.setValue(
                page.setRequisitesStep.innSearchInput(),
                VALID_INN
            );
            await bro.yaWaitForVisible(
                page.setRequisitesStep.expandRequisites(),
                'Не подтянулись реквизиты'
            );
            await bro.click(page.setRequisitesStep.expandRequisites());
            await bro.assertView(
                'diskforbusiness-593-3',
                page.setRequisitesStep()
            );
        });
        it('diskforbusiness-595: Регистрация организации клиента с ручным вводом реквизитов', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-1',
                'pass-yndx-sarah-partner-1'
            );
            await gotoMasterRegistration(bro, { partner: 1 });
            await bro.assertView('diskforbusiness-595-1', page.root());
            await bro.click(page.chooseOwnerStep.continue());
            await bro.assertView('diskforbusiness-595-2', page.root());

            await bro.setValue(
                page.setRequisitesStep.innSearchInput(),
                INVALID_INN
            );
            await bro.yaWaitForVisible(
                page.setRequisitesStep.manualSetRequisites(),
                'Не появилась кнопка ручного ввода реквизитов'
            );
            await bro.assertView(
                'diskforbusiness-595-3',
                page.setRequisitesStep()
            );

            await bro.click(page.setRequisitesStep.manualSetRequisites());
            await bro.$(page.setRequisitesStep()).moveTo(0, 0);
            await bro.assertView(
                'diskforbusiness-595-4',
                page.setRequisitesStep()
            );
        });
        it('diskforbusiness-596: Регистрация организации клиента с автозаполнением реквизитов', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-1',
                'pass-yndx-sarah-partner-1'
            );
            await gotoMasterRegistration(bro, { partner: 1 });
            await bro.assertView('diskforbusiness-596-1', page.root());
            await bro.click(page.chooseOwnerStep.continue());
            await bro.assertView('diskforbusiness-596-2', page.root());
            await bro.setValue(
                page.setRequisitesStep.innSearchInput(),
                VALID_INN
            );
            await bro.yaWaitForVisible(
                page.setRequisitesStep.expandRequisites(),
                'Не подтянулись реквизиты'
            );
            await bro.click(page.setRequisitesStep.expandRequisites());
            await bro.assertView(
                'diskforbusiness-596-3',
                page.setRequisitesStep()
            );
        });

        it('diskforbusiness-594: Попытка регистрации организации партнера (уже есть организация), diskforbusiness-598: Переход к списку организаций из заглушки организации-партнера', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-2',
                'pass-yndx-sarah-partner-2'
            );
            await gotoMasterRegistration(bro, { soft: true });
            await bro.yaWaitForVisible(
                page.partnerNotAvailable(),
                'Не отобразилась заглушка о недоступности регистрации партнёрской организации'
            );
            await bro.assertView(
                'diskforbusiness-594-1',
                page.partnerNotAvailable()
            );
            await bro.click(page.partnerNotAvailable.toOrgsButton());
            await bro.assertView(
                'diskforbusiness-598-1',
                admin.organizationSelectionContainer()
            );
            await gotoOrganization(bro, true);
            await bro.yaWaitForVisible(
                common.navigationMenu(),
                'Не отобразилось меню навигации'
            );
            await bro.assertView(
                'diskforbusiness-598-2',
                common.navigationMenu()
            );
        });

        it('diskforbusiness-597: Попытка регистрации организации клиента (уже есть организация), diskforbusiness-599: Переход к списку организаций из заглушки организации-клиента', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-partner-3',
                'pass-yndx-sarah-partner-3'
            );

            await gotoMasterRegistration(bro, { soft: true });
            await bro.yaWaitForVisible(
                page.partnersClientNotAvailable(),
                'Не отобразилась заглушка о недоступности регистрации клиентской организации'
            );
            await bro.assertView(
                'diskforbusiness-597-1',
                page.partnersClientNotAvailable()
            );
            await bro.click(page.partnersClientNotAvailable.toOrgsButton());
            await bro.assertView(
                'diskforbusiness-599-1',
                admin.organizationSelectionContainer()
            );
            await gotoOrganization(bro, true);
            await bro.yaWaitForVisible(
                common.navigationMenu(),
                'Не отобразилось меню навигации'
            );
            await bro.assertView(
                'diskforbusiness-599-2',
                common.navigationMenu()
            );
        });
    });
});
