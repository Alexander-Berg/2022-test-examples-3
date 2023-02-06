const { assert } = require('chai');
const { removeDepartment } = require('../../helpers/directoryApi');
const { adminUITest2 } = hermione.ctx.testUsers;

describe('Создание отдела', () => {
    beforeEach(function() {
        return this.browser
            // логинимся и переходим в админку
            .login({ ...adminUITest2, retpath: '/portal/admin' })
            // ждем появления футера в корневом отдел
            .waitForVisible('.department-footer')
            // кликаем в значок плюса в футере
            .click('.department-footer button')
            // ждем появления попапа
            .waitForVisible('.popup .menu .menu__group:nth-child(1) .menu-item[data-bem*="department"]')
            // выбираем второй пункт "Добавить сотрудника"
            .click('.popup .menu .menu__group:nth-child(1) .menu-item[data-bem*="department"]')
            .waitForVisible('.create-department-form');
    });

    afterEach(function() {
        const { cacheMode } = hermione.ctx;

        // В режиме для чтения ничего удалять не надо
        if (cacheMode === 'read') {
            return;
        }

        return this.browser.getMeta('newDepartment')
            .then(id => {
                if (!id) {
                    return;
                }

                return removeDepartment(id, adminUITest2);
            });
    });

    it('Отдел создается, если заполнены обязательные поля', function() {
        const departmentData = {
            name: 'Рога и копыт',
            label: 'winners3',
        };

        /* alias: 1-create */
        return this.browser
            .disableAnimations('*')

            .assertView('before-create', '.modal_visible .modal__content')

            .fillForm('create-department-form', departmentData)
            .click('.create-department-form .form__label')

            .assertView('filled-form', '.create-department-form', {
                ignoreElements: ['#department-form-label'],
            })

            .setHash('create-department')
            .click('.create-department-form button[type="submit"]')
            .waitForVisible('.department-section:not(.root-department-section)')

            .getUrl()
            .then(url => {
                const departmentId = url.match(/\d+$/)[0];

                this.browser.setMeta('newDepartment', Number(departmentId));
            })

            .getText('.department-section:nth-child(2) .department-header__title')
            .then(actualDepartmentName => {
                assert.equal(actualDepartmentName.toLowerCase(), departmentData.name.toLowerCase());
            })

            .waitForVisible('.notifications')
            .assertView('notification', '.notification-content')

            .click('.notifications .close')

            .waitForVisible('.root-department-section .section-list-item_selected .unit_type-department')
            .assertView('department-section', '.sections', {
                ignoreElements: ['.department-section:nth-child(2) .department-contacts .link'],
            });
    });

    it('Ошибки если не заполнены обязательные поля', function() {
        /* alias: 2-error */
        return this.browser
            .click('.create-department-form button[type="submit"]')
            .waitForVisible('.create-department-form .form__error')
            .assertView('create-department-form-errors', '.create-department-form');
    });
});
