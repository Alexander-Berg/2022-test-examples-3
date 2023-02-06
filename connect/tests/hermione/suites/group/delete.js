const parseUrl = require('url').parse;
const path = require('path');
const fs = require('fs');
const { assert } = require('chai');
const { getContext } = require('@yandex-int/infratest-utils/lib/hermione-get-context');

const { createGroup, readGroupList } = require('../../helpers/directoryApi');
const { testUsers: { adminUITest2 }, cacheMode } = hermione.ctx;

function writeId(id, testId) {
    fs.writeFileSync(path.join(__dirname, `delete_${testId}.json`), JSON.stringify({ groupId: id }, null, 4));
}

describe('Удаление команды', () => {
    beforeEach(function() {
        const newGroup = {
            name: 'Команда для удаления',
            label: `del-group-${Math.random().toString().substring(2, 7)}`,
        };

        const testId = getContext(this.browser.executionContext).id();

        let getGroupId;

        if (cacheMode !== 'read') {
            getGroupId = readGroupList(adminUITest2).then(({ result }) => {
                const groups = result.filter(({ type, label }) =>
                    type === 'generic' && label.startsWith('del-group-'));

                if (groups.length) {
                    writeId(groups[0].id, testId);

                    return groups[0].id;
                }

                return createGroup(newGroup, adminUITest2).then(({ id }) => {
                    writeId(id, testId);

                    return id;
                });
            });
        } else {
            const { groupId } = JSON.parse(fs.readFileSync(path.join(__dirname, `delete_${testId}.json`), 'utf-8'));

            getGroupId = Promise.resolve(groupId);
        }

        return getGroupId.then(id => this.browser
        // зайти под админом в карточку команды по ссылке /portal/admin/groups/<номер команды>
            .login({ ...adminUITest2, retpath: `/portal/admin/groups/${id}` })
            .waitForVisible('.loader_visible', 3000, true)

        // команда для удаления существует
            .getText('.group-section .section-header__title')
            .then(text => {
                assert.equal(text, newGroup.name);

                this.browser.setMeta('groupId', Number(id));
            }));
    });

    it('Команда удаляется', function() {
        /* alias: 1-success */
        return this.browser
            .disableAnimations('*')

            // нажать на кнопку "..."
            .click('.edit-group-controls .ui-flat-button')

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

            // отображается сообщение “Команда <название команды> удалена”
            .waitForVisible('.notification__success', 5000)

            // внешний вид сообщения [notification]
            .assertView('notification', '.notification__success .notification-content')

            // нажать на Крестик
            .click('.notification__success .ui-flat-button')

            // выполнился переход в корень команд /portal/admin/groups
            .getUrl()
            .then(url => {
                const parsedUrl = parseUrl(url);

                assert.equal(parsedUrl.pathname, '/portal/admin/groups');
            })
            // внешний вид списка команд [commands]
            .assertView('commands', '.group-list-body');
    });

    it('Отмена удаления команды', function() {
        /* alias: 2-cancel */
        return this.browser
            .disableAnimations('*')

            // нажать на кнопку "..."
            .click('.edit-group-controls .ui-flat-button')

            // нажать на кнопку "Удалить" в выпадающем меню
            .waitForVisible('.popup_visible .menu')
            .click('.popup_visible .menu .menu-item[data-bem*="remove"]')

            // дождаться появления формы
            .waitForVisible('.modal_visible .confirm-dialog-form')

            // нажать на кнопку "Отменить"
            .click('.confirm-dialog-form .form__buttons button[type="button"]')

            // переход не выполняется, остаемся на странице /portal/admin/groups/<номер команды>
            .getUrl()
            .then(url => {
                const parsedUrl = parseUrl(url);

                return this.browser.getMeta('groupId').then(id => {
                    assert.equal(parsedUrl.pathname, `/portal/admin/groups/${id}`);
                });
            })

            // внешний вид списка команд и карточки команды, все элементы отображаются правильно, попапа нет [commands]
            .assertView('commands', '.group-layout', {
                ignoreElements: [
                    '.group-header .list-item:nth-child(1) .list-item__value',
                    '.group-header .list-item:nth-child(3) .list-item__value',
                ],
            });
    });
});
