const fs = require('fs');
const parseUrl = require('url').parse;
const path = require('path');
const { assert } = require('chai');
const { getContext } = require('@yandex-int/infratest-utils/lib/hermione-get-context');

const { createDepartment, readDepartmentList } = require('../../helpers/directoryApi');
const { testUsers: { adminUITest2 }, cacheMode } = hermione.ctx;

function writeId(id, testId) {
    fs.writeFileSync(path.join(__dirname, `delete_${testId}.json`), JSON.stringify({ deptId: id }, null, 4));
}

describe('Удаление отдела без пользователей и подотделов', () => {
    beforeEach(function() {
        const newDpt = {
            name: 'Отдел для удаления',
            label: `delete-dept-${Math.random().toString().substring(2, 7)}`,
            parent_id: 1,
        };

        const testId = getContext(this.browser.executionContext).id();

        let getDeptId;

        if (cacheMode !== 'read') {
            getDeptId = readDepartmentList(adminUITest2).then(({ result }) => {
                const departments = result.filter(({ parent_id: parentId, label }) =>
                    parentId === newDpt.parent_id && label.startsWith('delete-dept-'));

                if (departments.length) {
                    writeId(departments[0].id, testId);

                    return departments[0].id;
                }

                return createDepartment(newDpt, adminUITest2).then(({ id }) => {
                    writeId(id, testId);

                    return id;
                });
            });
        } else {
            const { deptId } = JSON.parse(fs.readFileSync(path.join(__dirname, `delete_${testId}.json`), 'utf-8'));

            getDeptId = Promise.resolve(deptId);
        }

        return getDeptId.then(id => this.browser
        // зайти под админом в карточку отдела по ссылке /portal/admin/departments/<номер отдела>
            .login({ ...adminUITest2, retpath: `/portal/admin/departments/${id}` })
            .waitForVisible('.loader_visible', 3000, true)

        // департамент для удаления существует
            .getText('.department-section:last-child .department-header__title')
            .then(text => {
                assert.equal(text, newDpt.name.toLocaleUpperCase());

                this.browser.setMeta('deptId', Number(id));
            }));
    });

    it('Отдел удаляется', function() {
        /* alias: 1-del */
        return this.browser
            .disableAnimations('*')

            // нажать на кнопку "..."
            .click('.department-section:last-child .edit-department-controls .ui-flat-button')

            // отображается выподающее меню [menu]
            .waitForVisible('.popup_visible .menu')
            .assertView('menu', '.popup_visible .menu')

            // нажать на кнопку "Удалить" в выпадающем меню
            .click('.popup_visible .menu .menu-item[data-bem*="remove"]')

            // отображается форма с предложением удалить или отменить
            .waitForVisible('.modal_visible .confirm-dialog-form')
            // внешний вид формы [form]
            .assertView('form', '.confirm-dialog-form')

            // нажать на кнопку "Удалить"
            .click('.confirm-dialog-form .form__buttons button[type="submit"]')

            // отображается сообщение “Отдел удален”
            .waitForVisible('.notification__success', 5000)

            // внешний вид сообщения [notification]
            .assertView('notification', '.notification__success .notification-content')

            // нажать на Крестик
            .click('.notification__success .ui-flat-button')
            // выполнился переход в корневой отдел /portal/admin/departments/1
            .getUrl()
            .then(url => {
                const parsedUrl = parseUrl(url);

                assert.equal(parsedUrl.pathname, '/portal/admin/departments/1');
            })

            // внешний вид списка отделов и списка пользователей [departments]
            .assertView('departments', '.department-list');
    });

    it('Отмена удаления отдела', function() {
        /* alias: 2-cancel */
        return this.browser
            // нажать на кнопку "..."
            .click('.department-section:last-child .edit-department-controls .ui-flat-button')

            // нажать на кнопку "Удалить" в выпадающем меню
            .waitForVisible('.popup_visible .menu')
            .click('.popup_visible .menu .menu-item[data-bem*="remove"]')

            // дождаться появления формы
            .waitForVisible('.modal_visible .confirm-dialog-form')

            // нажать на кнопку "Отменить"
            .click('.confirm-dialog-form .form__buttons button[type="button"]')

            // переход не выполняется остаемся на странице /portal/admin/departments/<номер отдела>
            .getUrl()
            .then(url => {
                const parsedUrl = parseUrl(url);

                return this.browser.getMeta('deptId').then(id => {
                    assert.equal(parsedUrl.pathname, `/portal/admin/departments/${id}`);
                });
            })

            // внешний вид списка c отделами со списком сотрудников и карточка отдела,
            // отображаются правильно, формы удаления нет [departments]
            .assertView('departments', '.department-layout', {
                ignoreElements: [
                    '.department-section .department-contacts .list-item__value',
                ],
            });
    });
});
