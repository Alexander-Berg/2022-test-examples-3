const { testUsers: { adminYO2 } } = hermione.ctx;

const FormFieldMap = {
    address: 'content',
    target: 'content',
    strings: 'content',
    exchange: 'content',
    preference: 'priority',
};

function setDnsRecord(browser, record) {
    let chain = browser;

    Object.entries(record).forEach(([key, value]) => {
        chain = chain.then(() => {
            let inputSelector = `.edit-dns-record-modal__input_type_${FormFieldMap[key] || key} input`;

            return browser
                .clearInput(inputSelector)
                .setValue(inputSelector, value);
        });
    });

    return chain;
}

function checkDel(browser, type) {
    return browser
        // нажать на кнопку в виде корзины на строке с добавленной записью
        // https://st.yandex-team.ru/QAFW-2403
        // У меня на версии ff 61 тесты не запускаются, поэтому добавляю хак с уводом курсора
        .moveToObject('body', -1, -1)
        .moveToObject('.dns-settings__tbody-tr:first-child')
        .click('.dns-settings__tbody-tr:first-child .dns-settings__record-action_type_remove')
        // появилось окно "Уверены, что хотите удалить DNS-запись?"
        .waitForVisible('.modal_visible_yes.dns-settings__remove-record-modal')
        .setHash('dns-del')
        // нажать кнопку "Удалить"
        .click('.modal_visible_yes .modal__button_type_confirm')
        // отображается сообщение о том, что запись удалена [notification-del]
        .waitForVisible('.status-notification_type_success', 15000)
        .assertView('notification-del', '.status-notification')
        // нажать на крестик на сообщении
        .click('.status-notification__close-button')
        // запись удалилась
        .waitForVisible(`.dns-settings__record-type=${type}`, 1000, true)
        // вкладка управления ДНС без записи [dns-del]
        .assertView('dns-del', '.dns-settings');
}

function checkEdit(browser, type) {
    const record = {
        name: 'test2.host',
    };

    return browser
        // нажать на кнопку в виде карандаша на строке с добавленной записью
        // https://st.yandex-team.ru/QAFW-2403
        .moveToObject('body', -1, -1)
        .moveToObject('.dns-settings__tbody-tr:first-child')
        .click('.dns-settings__tbody-tr:first-child .dns-settings__record-action_type_edit')
        // открылось окно с предзаполненной формой
        .waitForVisible('.modal_visible_yes.edit-dns-record-modal')
        // внешний вид заполненной формы [form-edit]
        .assertView('form-edit', '.modal_visible_yes .modal__content')
        // в поле "Хост" ввести "test2.host"
        .then(() => setDnsRecord(browser, record))
        // нажать кнопку "Обновить"
        .setHash('dns-edit')
        .click('.modal_visible_yes .form__button_type_submit')
        // отображается сообщение о том, что запись обновлена [notification-edit]
        .waitForVisible('.status-notification_type_success', 15000)
        .assertView('notification-edit', '.status-notification')
        // нажать на крестик на сообщении
        .click('.status-notification__close-button')
        // данные в таблице обновились
        .waitForVisible(`.dns-settings__record-name=${record.name}`)
        .waitForVisible(`.dns-settings__record-type=${type}`)
        // вкладка управления ДНС с обновленной записью [dns-edit]
        .assertView('dns-edit', '.dns-settings');
}

const DnsRecord = {
    A: {
        name: 'test.host',
        address: '1.2.3.4',
        ttl: '1233',
    },
    CNAME: {
        name: 'test.host',
        target: '1.2.3.4',
        ttl: '1233',
    },
    AAAA: {
        name: 'test.host',
        address: '1:2:3:4:5:6:7:8',
        ttl: '1233',
    },
    TXT: {
        name: 'test.host',
        strings: 'testtesttest',
        ttl: '1233',
    },
    NS: {
        name: 'test.host',
        target: 'testtesttest',
        ttl: '1233',
    },
    MX: {
        name: 'test.host',
        exchange: 'testtesttest',
        preference: '1',
        ttl: '1233',
    },
    SRV: {
        name: 'test.host',
        target: 'testtesttest',
        priority: '1',
        weight: '2',
        port: '80',
        ttl: '1233',
    },
};

function checkAddEditDel(browser, type) {
    const record = DnsRecord[type];

    return browser
        // внешний вид незаполненной формы [form-plain]
        .assertView('form-plain', '.edit-dns-record-modal__form')
        // заполнить поля формы
        .then(() => setDnsRecord(browser, record))
        // внешний вид заполненной формы [form-filled]
        .assertView('form-filled', '.edit-dns-record-modal__form')
        // нажать на кнопку "Создать"
        .setHash('dns-add')
        .click('.edit-dns-record-modal__form .form__button_type_submit')
        // отображается сообщение о том, что запись создана [notification]
        .waitForVisible('.status-notification_type_success', 20000)
        .assertView('notification', '.status-notification')
        // нажать на крестик на сообщении
        .click('.status-notification__close-button')
        // запись добавилась
        .waitForVisible(`.dns-settings__record-name=${record.name}`)
        .waitForVisible(`.dns-settings__record-type=${type}`)
        // вкладка управления ДНС с добавленной записью [dns]
        .assertView('dns', '.dns-settings')
        .then(() => checkEdit(browser, type))
        .then(() => checkDel(browser, type));
}

function selectType(browser, type) {
    return browser
        .click('.edit-dns-record-modal__select_type_type')
        .waitForVisible('.popup2_visible_yes .menu')
        .click(`.menu__text=${type}`);
}

function openForm(browser) {
    return browser
        // нажать кнопку "Добавить DNS-запись"
        .click('.dns-settings__button_type_add-record')
        // открылось окно с формой "Создание A-записи"
        .waitForVisible('.modal_visible_yes.edit-dns-record-modal');
}

describe('Управление DNS', () => {
    beforeEach(function() {
        return this.browser
            // авторизоваться под администратором организации c подтвержденным доменом
            // перейти по ссылке с подтвержденным доменом
            .login({ ...adminYO2, retpath: '/portal/services/webmaster/resources/adminyo2.auto.connect-test.tk' })
            .waitForVisible('.domain-page__header', 30000)
            .waitForVisible('.dns-settings__actions', 30000)
            .setViewportSize({ width: 1280, height: 2000 })
            .disableAnimations('*')
            .disableBorderRadius('.status-notification, .modal__content')
            .hideCaret();
    });

    describe('Положительные', () => {
        it('1. Внешний вид страницы домена', function() {
            /* alias: pos-1-view */
            return this.browser
                // вид страницы домена с пустой вкладкой управления ДНС [plain]
                .assertView('plain', '.domain-page__content');
        });

        it('2. А-запись добавляется, редактируется и удаляется', function() {
            /* alias: pos-2-a */
            return this.browser
                .then(() => openForm(this.browser))
                // выбрать Тип записи "A"
                .then(() => selectType(this.browser, 'A'))
                .then(() => checkAddEditDel(this.browser, 'A'));
        });

        it('3. CNAME добавляется, редактируется и удаляется', function() {
            /* alias: pos-3-cname */
            return this.browser
                .then(() => openForm(this.browser))
                // выбрать Тип записи "CNAME"
                .then(() => selectType(this.browser, 'CNAME'))
                .then(() => checkAddEditDel(this.browser, 'CNAME'));
        });

        it('4. AAAA добавляется, редактируется и удаляется', function() {
            /* alias: pos-4-aaaa */
            return this.browser
                .then(() => openForm(this.browser))
                // выбрать Тип записи "AAAA"
                .then(() => selectType(this.browser, 'AAAA'))
                .then(() => checkAddEditDel(this.browser, 'AAAA'));
        });

        it('5. TXT-запись добавляется, редактируется и удаляется', function() {
            /* alias: pos-5-txt */
            return this.browser
                .then(() => openForm(this.browser))
                // выбрать Тип записи "TXT"
                .then(() => selectType(this.browser, 'TXT'))
                .then(() => checkAddEditDel(this.browser, 'TXT'));
        });

        it('6. NS-запись добавляется, редактируется и удаляется', function() {
            /* alias: pos-6-ns */
            return this.browser
                .then(() => openForm(this.browser))
                // выбрать Тип записи "NS"
                .then(() => selectType(this.browser, 'NS'))
                .then(() => checkAddEditDel(this.browser, 'NS'));
        });

        it('7. MX-запись добавляется, редактируется и удаляется', function() {
            /* alias: pos-7-mx */
            return this.browser
                .then(() => openForm(this.browser))
                // выбрать Тип записи "MX"
                .then(() => selectType(this.browser, 'MX'))
                .then(() => checkAddEditDel(this.browser, 'MX'));
        });

        it('8. SRV-запись добавляется, редактируется и удаляется', function() {
            /* alias: pos-8-srv */
            return this.browser
                .then(() => openForm(this.browser))
                // выбрать Тип записи "SRV"
                .then(() => selectType(this.browser, 'SRV'))
                .then(() => checkAddEditDel(this.browser, 'SRV'));
        });
    });

    describe('Отрицательные', () => {
        it('1. Сохранение пустой формы', function() {
            /* alias: neg-1-empty */
            return this.browser
                .hideModalBackground()
                .then(() => openForm(this.browser))
                // выбрать любой Тип записи
                .then(() => selectType(this.browser, 'A'))
                // нажать кнопку "Создать"
                .click('.edit-dns-record-modal__form .form__button_type_submit')
                // отображаются ошибки около незаполненных обязательных полей [errors]
                .assertView('errors', '.edit-dns-record-modal');
        });

        it('2. Отмена сохранения формы', function() {
            /* alias: neg-2-cancel */
            return this.browser
                .then(() => openForm(this.browser))
                // нажать кнопку "Отменить"
                .click('.edit-dns-record-modal__form .form__button_type_cancel')
                // окно с формой создания dns-записи закрылось
                .waitForVisible('.modal_visible_yes.edit-dns-record-modal', 1000, true);
        });
    });
});
