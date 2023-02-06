const {
    organizationPage,
    common
} = require('../../../hermione/pages/organization-profile');
const { gotoOrganization } = require('./helpers/common');

const { assert } = require('chai');
const path = require('path');
const _ = require('lodash');

async function goToOrgProfilePage(bro) {
    await gotoOrganization(bro);
    await bro.url('/company-profile');
}

async function deleteLogo(bro) {
    await bro.click(organizationPage.ChangeLogoSection.DeleteLogoButton());
    await bro.yaWaitForHidden(common.Spiner());
}

const orgPageAssertView = async (bro, title, selector) => {
    await bro.yaAssertView(title, selector, {
        hideElements: ['.Popup2']
    });
};

const makeOrgPageScreen = async bro => {
    await goToOrgProfilePage(bro);
    await orgPageAssertView(bro, 'org-profile-page', organizationPage());
};

describe('Профиль организации', function () {
    hermione.only.in('chrome-desktop');
    it('Внешний вид профиля организации для организации без домена', async function () {
        const bro = this.browser;

        await bro.yaLoginFast(
            'yndx-sarah-test-030',
            'pass-yndx-sarah-test-030'
        );
        await makeOrgPageScreen(bro);
    });

    hermione.only.in('chrome-desktop');
    hermione.auth.tus({ login: 'yndx-sarah-test-029', tus_consumer: 'sarah' });
    it('Переименование организации', async function () {
        const title = `Organization${_.random(0, 10000)}`;
        const bro = this.browser;
        await goToOrgProfilePage(bro);

        await bro.click(
            organizationPage.changeTitleSection.changeTitleButton()
        );
        await bro.pause(100);
        await bro.doubleClick(
            organizationPage.changeTitleSection.changeTitleInput()
        );
        await bro.keys('Back space');
        await bro.setValue(
            organizationPage.changeTitleSection.changeTitleInput(),
            title
        );
        await bro.click(
            organizationPage.changeTitleSection.changeTitleSubmit()
        );
        await bro.yaWaitForHidden(common.Spiner());

        const newTitle = await bro.getText(
            organizationPage.changeTitleSection.organizationTitle()
        );
        assert(title === newTitle, `Название организации не изменилось`);
    });

    hermione.only.in('chrome-desktop');
    hermione.auth.tus({ login: 'yndx-sarah-test-027', tus_consumer: 'sarah' });
    hermione.auth.tus({ login: 'yndx-sarah-test-028', tus_consumer: 'sarah' });
    it('Смена владельца', async function () {
        const bro = this.browser;
        const firstUser = 'yndx-sarah-test-027';
        const firstPassword = 'pass-yndx-sarah-test-027';
        const secondUser = 'yndx-sarah-test-028';
        const secondPassword = 'pass-yndx-sarah-test-028';
        let admin = firstUser;
        let user = secondUser;
        await bro.yaLoginFast(admin, firstPassword);
        await goToOrgProfilePage(bro);
        const isAdmin = await bro.isExisting(
            organizationPage.OrganizationOwnerSection.Button()
        );

        if (!isAdmin) {
            user = firstUser;
            admin = secondUser;
            await bro.yaLoginFast(admin, secondPassword);
            await goToOrgProfilePage(bro);
        }

        await bro.click(organizationPage.OrganizationOwnerSection.Button());
        await bro.pause(500); // анимация попапа
        await bro.addValue(common.modalInput(), user + '@yandex.ru');
        await bro.click(common.changeOwnerNextStepButton());
        await bro.click(common.changeOwnerConfirmChange());
        await bro.yaWaitForHidden(common.Spiner(), 15000);

        const newOwner = await bro.getText(
            organizationPage.OrganizationOwnerSection.login()
        );

        assert(newOwner === `${user}@yandex.ru`, `Владелец не сменилася`);
    });

    hermione.only.in('chrome-desktop');
    hermione.auth.tus({ login: 'yndx-sarah-test-031', tus_consumer: 'sarah' });
    it('Загрузка и удаление логотипа', async function () {
        const bro = this.browser;
        await goToOrgProfilePage(bro);

        var fileToUpload = path.join(__dirname, './files/org-logo.jpeg');

        const logoExist = await bro.isExisting(
            organizationPage.ChangeLogoSection.DeleteLogoButton()
        );

        if (logoExist) {
            await deleteLogo(bro);
        }

        await bro.yaAssertView(
            'organization-logo-without-logo',
            organizationPage.ChangeLogoSection()
        );

        await bro.chooseFile(
            organizationPage.ChangeLogoSection.ChangeLogoInput(),
            fileToUpload
        );
        await bro.yaWaitForHidden(common.Spiner());
        await bro.yaAssertView(
            'organization-logo-with-logo',
            organizationPage.ChangeLogoSection()
        );
        await bro.yaAssertView(
            'organization-header-with-logo',
            '.PSHeader-Left'
        );

        await deleteLogo(bro);
        await bro.yaAssertView(
            'organization-logo-after-delete-logo',
            organizationPage.ChangeLogoSection()
        );
    });
});

describe('Удаление организации', function () {
    describe('Не отображать кнопку', function () {
        hermione.only.in('chrome-desktop');
        hermione.auth.tus({
            login: 'yndx-sarah-test-041',
            tus_consumer: 'sarah'
        });
        it('В организации несколько пользователей', async function () {
            const bro = this.browser;
            await goToOrgProfilePage(bro);
            await makeOrgPageScreen(bro);
        });

        hermione.only.in('chrome-desktop');
        hermione.auth.tus({
            login: 'yndx-sarah-test-039',
            tus_consumer: 'sarah'
        });
        it('В организации есть есть домен', async function () {
            const bro = this.browser;
            await goToOrgProfilePage(bro);
            await makeOrgPageScreen(bro);
        });
    });

    describe('Попап удаления организации', function () {
        hermione.only.in('chrome-desktop');
        hermione.auth.tus({
            login: 'yndx-sarah-test-040',
            tus_consumer: 'sarah'
        });
        it('Кнопка отобразилась', async function () {
            const bro = this.browser;
            await goToOrgProfilePage(bro);
            await makeOrgPageScreen(bro);
        });

        hermione.only.in('chrome-desktop');
        hermione.auth.tus({
            login: 'yndx-sarah-test-040',
            tus_consumer: 'sarah'
        });
        it('Попап удаления', async function () {
            const bro = this.browser;
            await goToOrgProfilePage(bro);

            await bro.click(organizationPage.DeleteButton());
            await bro.yaWaitForVisible(common.modal());
            // Анимация
            await bro.pause(300);
            await bro.yaAssertView('delete-popup-1', common.modal());

            await bro.click(common.modalDeleteCheckbox());
            // Анимация
            await bro.pause(300);
            await bro.yaAssertView('delete-popup-2', common.modal());

            await bro.click(common.modalCancelButton());
            await bro.yaWaitForHidden(
                common.modal(),
                'Не скрылся попап удаления'
            );
        });
    });
});
