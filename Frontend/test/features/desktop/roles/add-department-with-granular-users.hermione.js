const assert = require('assert');
const PO = require('./PO');

describe('Добавление в команду', function() {
    describe('Положительные', function() {
        it('1. Добавление нового департамента с участниками с ограниченными правами (бессрочное)', async function() {
            const searchString = 'роботы внутренних';
            const searchResult = 'Роботы внутренних сервисов';

            // открыть страницу сервиса "autotest-add-departments"
            await this.browser
                .openIntranetPage({ pathname: '/services/autotest-add-departments' })
                .waitForVisible('.service-team .service-team-scope', 10000)
                // нажать кнопку добавления в команду
                .click(PO.addTeamMember());

            // в поле ввода "Добавить сотрудника или департамент" ввести "роботы внутренних"
            await this.browser.setValue(PO.teamEditor.addMemberField.control(), searchString);

            // под полем ввода появился выпадающий список с вариантом "Роботы внутренних сервисов"
            await this.browser.waitForVisible(PO.visiblePopup.firstDepItem(), 10000);
            const text = await this.browser.getText(PO.visiblePopup.firstDepartmentItem.departmentName());
            assert.strictEqual(text, searchResult);

            // кликнуть по варианту "Роботы внутренних сервисов"
            await this.browser.click(PO.visiblePopup.firstDepartmentItem());
            // в форме появился блок с запросом роли на департамент
            await this.browser.waitForVisible(PO.teamEditor.departmentEditor.firstDepartment());

            // указан департамент "Роботы внутренних сервисов"
            const addedMember = await this.browser.getText(PO.teamEditor.departmentEditor.firstDepartment.depName());
            assert.strictEqual(addedMember, searchResult);

            // есть подсказка с ворнингом про ограниченные роли
            await this.browser.waitForVisible(PO.teamEditor.rolesWarning(), 1000);

            // рубильник срока по умолчанию имеет значение "Бессрочно"
            const checkedExpiration = await this.browser.getValue(PO.teamEditor.expiration.checkedOption.control());
            assert.strictEqual(checkedExpiration, 'permanent');

            // роль для группы по умолчанию не выбрана
            const placeholder = await this.browser.getAttribute(
                PO.teamEditor.departmentEditor.firstDepartment.control.input(), 'placeholder');
            assert.strictEqual(placeholder, 'Выберите роль для группы');

            // форма добавления департамента с участниками с ограниченными правами
            await this.browser.assertView('add-department-form', PO.teamEditor());

            // кликнуть в поле "Выберите роль для группы"
            await this.browser.click(PO.teamEditor.departmentEditor.firstDepartment.control.role());

            // под полем появился выпадающий список с ролями
            await this.browser.waitForVisible(PO.visiblePopup.roleItem(), 10000);

            // выбрать из выпадающего списка ролей роль "Аналитик"
            await this.browser.click(PO.visiblePopup.analystRole());

            // в поле "Выберите роль для группы" подставилось значение "Аналитик"
            const createdRole = await this.browser.getValue(
                PO.teamEditor.departmentEditor.firstDepartment.control.input());
            assert.strictEqual(createdRole, 'Аналитик');

            // чтобы список участников обновился (запрос по тому же урлу, меняем хеш дампа)
            await this.browser.setHash('department-added');

            // нажать кнопку "Добавить"
            await this.browser.click(PO.teamEditor.submit());

            // попап исчез
            await this.browser.waitForVisible(PO.team.popup(), 10000, true);

            // появилась плашка "Запрошены роли"
            // с добавленным департаментом "Департамент / Роботы внутренних сервисов"
            await this.browser.waitForVisible(PO.team.unapproved(), 10000);
            const depName = await this.browser.getText(PO.team.unapproved.departmentName());
            assert.strictEqual(depName, 'Департамент / Роботы внутренних сервисов (86)');
        });
    });
});
