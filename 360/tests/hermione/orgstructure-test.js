const adminPage = require('../../../hermione/pages/admin');
const { gotoOrganization } = require('./helpers/common');

const ORGSTRUCTURE_TEST_ID = '503120';

hermione.only.in('chrome-desktop');

async function goToGroups(bro) {
    await bro.yaLoginFast('yandex-team-88200.24971', 'sUdY.c9L8');
    await gotoOrganization(bro);
    await bro.url(`/users/teams`);

    await bro.yaWaitForVisible(
        adminPage.groups(),
        'Не отобразилось дерево проектов'
    );
    await bro.click(adminPage.expandButton(1));
}

describe('Оргструктура', function () {
    describe('Оргструктура - основное', function () {
        it('orgstructure-1: онбординги оргструктуры - сотрудники, подразделения, проекты', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-016', 'gfhjkm13-gfhjkm13');
            await bro.url(`/users?test-id=${ORGSTRUCTURE_TEST_ID}`);
            await gotoOrganization(bro);

            await bro.yaWaitForVisible(
                adminPage.users.onboardingTitle(),
                'Не отобразился онбординг сотрудников'
            );
            await bro.assertView('users-onboarding', adminPage.users());

            await bro.click(adminPage.departmentsTab());
            await bro.yaWaitForVisible(
                adminPage.departments.onboardingTitle(),
                'Не отобразился онбординг подразделений'
            );
            await bro.assertView(
                'departments-onboarding',
                adminPage.departments()
            );

            await bro.click(adminPage.groupsTab());
            await bro.yaWaitForVisible(
                adminPage.groups.onboardingTitle(),
                'Не отобразился онбординг проектов'
            );
            await bro.assertView('groups-onboarding', adminPage.groups());
        });

        it('orgstructure-2: отображение подразделений', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-017', 'gfhjkm13-gfhjkm13');
            await gotoOrganization(bro);
            await bro.url(`/users/department?test-id=${ORGSTRUCTURE_TEST_ID}`);

            await bro.yaWaitForVisible(
                adminPage.departments(),
                'Не отобразилось дерево подразделений'
            );
            await bro.assertView('departments-root', adminPage.departments());

            await bro.click(adminPage.expandButton(1));
            await bro.click(adminPage.expandButton(1));
            await bro.click(adminPage.expandButton(1));
            await bro.yaWaitForVisible(adminPage.treeUser());

            await bro.assertView(
                'departments-expanded',
                adminPage.departments()
            );
        });

        it('orgstructure-3: отображение проектов', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-017', 'gfhjkm13-gfhjkm13');
            await gotoOrganization(bro);
            await bro.url(`/users/teams?test-id=${ORGSTRUCTURE_TEST_ID}`);

            await bro.yaWaitForVisible(
                adminPage.groups(),
                'Не отобразилось дерево проектов'
            );
            await bro.assertView('projects-root', adminPage.groups(), {
                hideElements: [adminPage.popup()]
            });

            await bro.click(adminPage.expandButton(1));
            await bro.click(adminPage.expandButton(1));
            await bro.yaWaitForVisible(adminPage.treeUser());

            await bro.assertView('groups-expanded', adminPage.groups(), {
                hideElements: [adminPage.popup()]
            });
        });
    });
    describe('Оргструктура - CHEMODAN-82598: Временное решение для отделов, которые были добавлены в команду', function () {
        it('diskforbusiness-487: Отображение подразделения в группе', async function () {
            const bro = this.browser;
            await goToGroups(bro);
            await bro.yaWaitForVisible(
                adminPage.groupDepartmentTreeListItem(),
                'Не отобразилось подразделение'
            );
            await bro.yaWaitForVisible(adminPage.treeUser());
            await bro.assertView('groups-department', adminPage.groups(), {
                hideElements: [adminPage.popup()]
            });
        });

        it('diskforbusiness-501: Отображение тултипа на вкладке Группы', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-sarah-test-017', 'gfhjkm13-gfhjkm13');
            await gotoOrganization(bro);
            await bro.url(`/users/teams?test-id=${ORGSTRUCTURE_TEST_ID}`);

            await bro.yaWaitForVisible(adminPage.groups());
            await bro.yaWaitForVisible(
                adminPage.tooltipButton(),
                'Не отобразился тултип'
            );
            await bro.assertView(
                'projects-root-with-tooltip',
                adminPage.groups()
            );

            await bro.click(adminPage.expandButton(1));
            await bro.click(adminPage.expandButton(1));
            await bro.yaWaitForVisible(adminPage.treeUser());

            await bro.assertView(
                'groups-expanded-with-tooltip',
                adminPage.groups()
            );
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83486');
        it('diskforbusiness-489: По ховеру показываем только действие удаления', async function () {
            const bro = this.browser;
            await goToGroups(bro);
            await bro.yaWaitForVisible(
                adminPage.groupDepartmentTreeListItem(),
                'Не отобразилось подразделение'
            );
            await bro.moveToObject(adminPage.groupDepartmentTreeListItem());
            await bro.assertView(
                'groups-department-hover',
                adminPage.groups(),
                {
                    hideElements: [adminPage.popup()],
                    withHover: true
                }
            );
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83486');
        it('diskforbusiness-491: Возможность удаления только корневого подразделения', async function () {
            const bro = this.browser;
            await goToGroups(bro);
            await bro.yaWaitForVisible(
                adminPage.groupDepartmentTreeListItem(),
                'Не отобразилось подразделение'
            );
            await bro.click(adminPage.groupDepartmentTreeListItem());
            await bro.assertView('groups-department-open', adminPage.groups(), {
                hideElements: [adminPage.popup()]
            });
            await bro.moveToObject(
                adminPage.groupDepartmentTreeListItem.child()
            );
            await bro.assertView(
                'groups-department-child-hover',
                adminPage.groups(),
                { withHover: true, hideElements: [adminPage.popup()] }
            );
        });

        //Покрыто частично, полностью нельзя т.к. добавить можно только из коннекта
        it('diskforbusiness-490: Удаление подразделения из группы', async function () {
            const bro = this.browser;
            await goToGroups(bro);
            await bro.yaWaitForVisible(
                adminPage.groupDepartmentTreeListItem(),
                'Не отобразилось подразделение'
            );
            await bro.click(
                adminPage.groupDepartmentTreeListItem.deleteButton()
            );
            await bro.yaWaitForVisible(
                adminPage.deleteModal(),
                'Не отобразилось модальное окно удаления департамента'
            );
            await bro.assertView(
                'groups-department-delete-modal-open',
                'body',
                {
                    hideElements: [adminPage.popup()]
                }
            );
            await bro.click(adminPage.deleteModal.closeButton());
            await bro.yaWaitForHidden(
                adminPage.deleteModal(),
                'Не убралось модальное окно удаления департамента'
            );
        });
    });
});
