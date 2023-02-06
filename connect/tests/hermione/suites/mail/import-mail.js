/* eslint-disable max-len */
const fs = require('fs');
const { join } = require('path');
const { assert } = require('chai');
const { testUsers: { adminYO2: admin } } = hermione.ctx;
const { CONNECT_TOKENS__USER_PASS: pass } = process.env;
const fileName = 'mail_import.csv';
const filePath = join(__dirname, fileName);

function assertText(browser, selector, text) {
    return browser
        .getValue(selector)
        .then(val => {
            assert.equal(val, text);
        });
}

function assertPortText(browser, text) {
    return browser
        .then(() => assertText(browser, '.mail-server-setup__port input[type=text]', text));
}

function assertPortHintText(browser, text) {
    return browser
        .getText('.mail-server-setup__description_subject_port')
        .then(val => {
            assert.equal(val, text);
        });
}

function assertServerText(browser, text) {
    return browser
        .then(() => assertText(browser, '.mail-server-setup__server input[type=text]', text));
}

function selectPreset(browser, preset, attrValue) {
    return browser
        .click(`.mail-server-setup__preset-radio=${preset}`)
        .getAttribute('.mail-server-setup__preset-radio.radio-button__radio_checked_yes input', 'value')
        .then(text => {
            assert.equal(text, attrValue);
        });
}

function selectProtocol(browser, protocol) {
    return browser
        .click(`.mail-server-setup__protocol-radio=${protocol}`)
        .getText('.mail-server-setup__protocol-radio.radio-button__radio_checked_yes .radio-button__text')
        .then(text => {
            assert.equal(text, protocol);
        });
}

function selectInputMode(browser, mode) {
    return browser
        .click(`.mail-import-list__mode-radio=${mode}`)
        .getText('.mail-import-list__mode-radio.radio-button__radio_checked_yes .radio-button__text')
        .then(text => {
            assert.equal(text, mode);
        });
}

function fillImportData(browser, row) {
    return browser
        // ввести почту Mail.ru в поле "Почта"
        .setValue(
            `.mail-import-list-editor__row_tbody:nth-child(${row}) .mail-import-list-editor__email input`,
            'ui.tester.connect@mail.ru'
        )
        // ввести пароль от почты в поле "Пароль"
        .setValue(
            `.mail-import-list-editor__row_tbody:nth-child(${row}) .mail-import-list-editor__password input`,
            pass
        );
}

function assertFileImportDisabled(browser) {
    return browser
        // кнопка "Начать перенос почты" задизейблена
        .getAttribute('.mail-import-file-upload__button_type_submit', 'disabled')
        .then(disabled => {
            assert.strictEqual(disabled, 'true');
        })

        // отображаются кнопка "Загрузить файл" с подсказкой "Загрузите файл в формате .csv объёмом не более 1 МБ"
        .waitForVisible('.mail-import-file-upload__attach')
        .waitForExactText('.mail-import-file-upload__attach .attach__text', 'Загрузите файл в формате .csv объёмом не более 1 МБ', 1000, true);
}

function assertImportSuccess(browser) {
    return browser
        // выполнился переход на "Перенос почтовых ящиков"
        .waitForVisible('.mail-import-progress')

        // появилась первая зеленая галка "Сверка данных"
        .waitForExactText('.mail-import-progress__stage_state_success', 'Сверка данных', 5000)

        // появилась вторая зеленая галка "Заведение новых учётных записей"
        .waitForExactText('.mail-import-progress__stage_state_success', 'Заведение новых учётных записей', 150000)

        // появилась третья зеленая галка "Настройка переноса почты на новые ящики"
        .waitForExactText('.mail-import-progress__stage_state_success', 'Настройка переноса почты на новые ящики', 300000)

        // внешний вид вкладки переноса [progress]
        .assertView('progress', '.mail-import-progress');
}

function assertServerSettings(browser, server) {
    return browser
        // в поле "Протокол" предвыбранна вкладка IMAP
        .getText('.mail-server-setup__protocol-radio.radio-button__radio_checked_yes')
        .then(text => {
            assert.equal(text, 'IMAP');
        })

        // стоит галка "Использовать SSL-шифрование"
        .waitForExist('.mail-server-setup__ssl.checkbox_checked_yes')

        // поле "Сервер" содержит значение imap.server
        .then(() => assertServerText(browser, `imap.${server}`))

        // поле "Порт" содержит значение 993
        .then(() => assertPortText(browser, '993'))

        // отображается подсказка под полем "Порт", текст "Стандартный порт — 993"
        .then(() => assertPortHintText(browser, 'Стандартный порт — 993'))

        // в поле "Настройки переноса почты" проставлены три галки
        .waitForExist('.mail-server-setup__keep-original.checkbox_checked_yes')
        .waitForExist('.mail-server-setup__import-contacts.checkbox_checked_yes')
        .waitForExist('.mail-server-setup__mark-original-as-read.checkbox_checked_yes')

        // выбрать вкладку POP3 в поле "Протокол"
        .then(() => selectProtocol(browser, 'POP3'))

        // изменился текст под полем "Порт", текст "Стандартный порт — 995"
        .then(() => assertPortHintText(browser, 'Стандартный порт — 995'))

        // поле "Сервер" содержит значение pop.server
        .then(() => assertServerText(browser, `pop.${server}`))

        // поле "Порт" содержит значение 995
        .then(() => assertPortText(browser, '995'))

        // кнопка "Дальше" активна
        .getAttribute('.mail-server-setup__button_type_next', 'disabled')
        .then(disabled => {
            assert.isNull(disabled);
        });
}

describe('Импорт почты', () => {
    beforeEach(function() {
        return this.browser
            // авторизоваться под учеткой в паспорте у которой есть организация с подтвержденным доменом
            // перейти на страницу сервиса "Почта" по ссылке {{CONNECT_HOST}}/portal/services/mail?noredirect=1
            .login({ ...admin, retpath: '/portal/services/mail?noredirect=1' })
            .waitForVisible('.tab-link__group', 3000)
            // перейти на вкладку "Импорт почты"
            .click('.tab-link=Импорт почты')
            .waitForVisible('.mail-import_inited', 10000);
    });

    describe('Положительные', () => {
        hermione.skip.in(/.*/, 'Убрали импорт из Коннекта DIR-10291');
        it('1. Сервер импорта "Свой сервер"', function() {
            /* alias: pos-1-own */
            return this.browser
                // внешний вид вкладки "Импорт почты" [plain]
                .assertView('plain', '.mail-import')

                // предвыбранна вкладка "Свой сервер" в блоке "Выберите сервер импорта"
                .then(() => selectPreset(this.browser, 'Свой сервер', 'custom'))

                // в поле "Протокол" предвыбранна вкладка IMAP
                .getText('.mail-server-setup__protocol-radio.radio-button__radio_checked_yes')
                .then(text => {
                    assert.equal(text, 'IMAP');
                })

                // стоит галка "Использовать SSL-шифрование"
                .waitForExist('.mail-server-setup__ssl.checkbox_checked_yes')

                // поле "Сервер" пустое
                .then(() => assertServerText(this.browser, ''))

                // поле "Порт" пустое
                .then(() => assertPortText(this.browser, ''))

                // отображается подсказка под полем "Порт", текст "Стандартный порт — 993"
                .then(() => assertPortHintText(this.browser, 'Стандартный порт — 993'))

                // в поле "Настройки переноса почты" проставлены три галки
                .waitForExist('.mail-server-setup__keep-original.checkbox_checked_yes')
                .waitForExist('.mail-server-setup__import-contacts.checkbox_checked_yes')
                .waitForExist('.mail-server-setup__mark-original-as-read.checkbox_checked_yes')

                // кнопка "Дальше" задизейблена
                .getAttribute('.mail-server-setup__button_type_next', 'disabled')
                .then(disabled => {
                    assert.strictEqual(disabled, 'true');
                })

                // отжать галку "Использовать SSL-шифрование"
                .click('.mail-server-setup__ssl')
                .waitForExist('.mail-server-setup__ssl.checkbox:not(.checkbox_checked_yes)')

                // изменился текст под полем "Порт", текст "Стандартный порт — 143"
                .then(() => assertPortHintText(this.browser, 'Стандартный порт — 143'))

                // нажать на подчеркнутый пунктиром текст "143"
                .click('.mail-server-setup__description_subject_port .value')

                // номер порта 143 прописался в поле "Порт"
                .then(() => assertPortText(this.browser, '143'))

                // выбрать вкладку POP3 в поле "Протокол"
                .click('.mail-server-setup__protocol-radio_type_pop3')
                .getText('.mail-server-setup__protocol-radio.radio-button__radio_checked_yes')
                .then(text => {
                    assert.equal(text, 'POP3');
                })

                // изменился текст под полем "Порт", текст "Стандартный порт — 110"
                .then(() => assertPortHintText(this.browser, 'Стандартный порт — 110'))

                // поставить галку "Использовать SSL-шифрование"
                .click('.mail-server-setup__ssl')
                .waitForExist('.mail-server-setup__ssl.checkbox_checked_yes')

                // изменился текст под полем "Порт", текст "Стандартный порт — 995"
                .then(() => assertPortHintText(this.browser, 'Стандартный порт — 995'))

                // заполнить поле "Сервер", например test.ru
                .setValue('.mail-server-setup__server input[type=text]', 'test.ru')

                // заполнить поле "Порт", например 995
                .setValue('.mail-server-setup__port input[type=text]', '995')

                // кнопка "Дальше" стала активной
                .getAttribute('.mail-server-setup__button_type_next', 'disabled')
                .then(disabled => {
                    assert.isNull(disabled);
                });
        });

        hermione.skip.in(/.*/, 'Убрали импорт из Коннекта DIR-10291');
        it('2. Сервер импорта "Mail.ru"', function() {
            /* alias: pos-2-mail */
            return this.browser
                // перейти на вкладку "Mail.ru" в блоке "Выберите сервер импорта"
                .then(() => selectPreset(this.browser, 'Mail.ru', 'mail_ru'))
                .then(() => assertServerSettings(this.browser, 'mail.ru'));
        });

        hermione.skip.in(/.*/, 'Убрали импорт из Коннекта DIR-10291');
        it('3. Сервер импорта "Gmail"', function() {
            /* alias: pos-3-gmail */
            return this.browser
                // перейти на вкладку "Gmail" в блоке "Выберите сервер импорта"
                .then(() => selectPreset(this.browser, 'Gmail', 'gmail'))
                .then(() => assertServerSettings(this.browser, 'gmail.com'));
        });

        hermione.skip.in(/.*/, 'Импорт не работает');
        it('4. Вручную', function() {
            /* alias: pos-4-manual */
            return this.browser
                .disableAnimations('*')

                // перейти на вкладку "Mail.ru" в блоке "Выберите сервер импорта"
                .then(() => selectPreset(this.browser, 'Mail.ru', 'mail_ru'))

                // нажать кнопку "Дальше"
                .click('.mail-server-setup__button_type_next')

                // открыта страница импорта вручную [plain]
                .waitForVisible('.mail-import-list', 30000)
                .assertView('plain', '.mail-import-list')

                // предвыбранна вкладка "Вручную"
                .getText('.mail-import-list__mode-radio.radio-button__radio_checked_yes')
                .then(text => {
                    assert.equal(text, 'Вручную');
                })

                // обязательные поля "Почта" и "Пароль" пустые
                .getValue('.mail-import-list-editor__row_tbody:nth-child(1) .mail-import-list-editor__email input')
                .then(val => {
                    assert.equal(val, '');
                })

                .getValue('.mail-import-list-editor__row_tbody:nth-child(1) .mail-import-list-editor__password input')
                .then(val => {
                    assert.equal(val, '');
                })

                // необязательное поле "Сотрудник в Коннекте" пустое
                .getValue('.mail-import-list-editor__row_tbody:nth-child(1) .mail-import-list-editor__user input')
                .then(val => {
                    assert.equal(val, '');
                })

                // кнопка "Начать перенос почты" неактивная
                .getAttribute('.mail-import-list-editor__button_type_submit', 'disabled')
                .then(disabled => {
                    assert.equal(disabled, 'true');
                })

                .then(() => fillImportData(this.browser, 1))

                // кнопка "Начать перенос почты" активная
                .getAttribute('.mail-import-list-editor__button_type_submit', 'disabled')
                .then(disabled => {
                    assert.isNull(disabled);
                })

                // пароль отображается в виде точек
                .waitForVisible('.mail-import-list-editor__row_tbody:nth-child(1) .mail-import-list-editor__password input[type=password]')

                // нажать на иконку с глазом рядом с заголовком колонки Пароль
                .click('.mail-import-list-editor__password-mask')

                // пароль отображается как есть, не замаскированный точками
                .waitForVisible('.mail-import-list-editor__row_tbody:nth-child(1) .mail-import-list-editor__password input[type=text]')

                // нажать на "Добавить почту"
                .click('.mail-import-list-editor__button_type_add-item')

                // добавились еще поля "Почта", "Пароль", "Сотрудник в Коннекте"
                .waitForVisible('.mail-import-list-editor__row_tbody:nth-child(2) .mail-import-list-editor__email')
                .waitForVisible('.mail-import-list-editor__row_tbody:nth-child(2) .mail-import-list-editor__password')
                .waitForVisible('.mail-import-list-editor__row_tbody:nth-child(2) .mail-import-list-editor__user')

                .then(() => fillImportData(this.browser, 2))

                // в конце строки отображается иконка корзины
                .waitForVisible('.mail-import-list-editor__row_tbody:nth-child(2) .mail-import-list-editor__button_type_remove-item')

                // нажать на иконку корзины
                .click('.mail-import-list-editor__row_tbody:nth-child(2) .mail-import-list-editor__button_type_remove-item')

                // строка удалилась
                .waitForVisible('.mail-import-list-editor__row_tbody:nth-child(2) .mail-import-list-editor__email', 1000, true)

                // появилась нотификация "Почта сотрудника удалена" с экшеном "Отменить"
                .waitForVisible('.status-notification_type_success', 15000)

                // нажать на экшен "Отменить" в нотификации
                .click('.status-notification__action_role_cancel')

                // удаленная строка восстановилась вместе с данными
                .waitForVisible('.mail-import-list-editor__row_tbody:nth-child(2) .mail-import-list-editor__email')

                // нажать на иконку корзины в этой строке
                .click('.mail-import-list-editor__row_tbody:nth-child(2) .mail-import-list-editor__button_type_remove-item')

                // строка удалилась
                .waitForVisible('.mail-import-list-editor__row_tbody:nth-child(2) .mail-import-list-editor__email', 1000, true)

                // появилась нотификация "Почта сотрудника удалена" с экшеном "Отменить"
                .waitForVisible('.status-notification_type_success', 15000)

                // нажать крестик в нотификации
                .click('.status-notification__close-button')

                // нажать кнопку "Начать перенос почты"
                .click('.mail-import-list-editor__button_type_submit')
                .then(() => assertImportSuccess(this.browser));
        });

        hermione.skip.in(/.*/, 'Импорт не работает');
        it('5. Через файл', function() {
            /* alias: pos-5-file */
            const header = 'email,password,first_name,last_name,new_login,new_password';
            const email = 'ui.tester.connect@mail.ru';
            const text = `${header}\n${email},${pass},Jane,Smith,jane.smith3,${pass}`;
            const hintText = 'Сформируйте текстовый файл. В каждой его строке укажите логин и пароль ' +
                'в кавычках и через запятую. Например: "email@yandex.ru","password"';

            fs.writeFileSync(filePath, text, null, 4);

            return this.browser
                // перейти на вкладку "Mail.ru" в блоке "Выберите сервер импорта"
                .then(() => selectPreset(this.browser, 'Mail.ru', 'mail_ru'))

                // нажать кнопку "Дальше"
                .click('.mail-server-setup__button_type_next')
                .waitForVisible('.mail-import-list', 30000)

                // перейти на вкладку "Из CSV-файла"
                .then(() => selectInputMode(this.browser, 'Из CSV-файла'))

                // открыта вкладка импорта "Из CSV-файла" [plain]
                .waitForVisible('.mail-import-file-upload')
                .assertView('plain', '.mail-import-file-upload')
                .then(() => assertFileImportDisabled(this.browser))

                // отображается подсказка "Сформируйте текстовый файл. В каждой его строке укажите логин и пароль в кавычках и через запятую.
                // Например: "email@yandex.ru","password"" над кнопкой "Скачать шаблон"
                .waitForExactText('.mail-import-file-upload__file-description-text', hintText, 1000, true)

                // есть кнопка "Скачать шаблон"
                .waitForVisible('.mail-import-file-upload__sample-file-link')

                // загрузить подготовленный корректный файл(см description) по кнопке "Загрузить файл"
                .chooseFile('.mail-import-file-upload__attach input[type="file"]', filePath)

                // отображается название загруженного файла
                .waitForExactText('.mail-import-file-upload__attach .attach__text', fileName, 1000, true, '')

                // рядом с файлом отображается кнопка с крестиком
                .waitForVisible('.mail-import-file-upload__attach .attach__reset')

                // кнопка "Начать перенос почты" активна
                .getAttribute('.mail-import-file-upload__button_type_submit', 'disabled')
                .then(disabled => {
                    assert.isNull(disabled);
                })

                // открыта страница импорта с загруженным файлом [file]
                .assertView('file', '.mail-import-file-upload')

                // нажать на крестик рядом с загруженным файлом
                .click('.mail-import-file-upload__attach .attach__reset')
                .then(() => assertFileImportDisabled(this.browser))

                // загрузить подготовленный корректный файл(см description) по кнопке "Загрузить файл"
                .chooseFile('.mail-import-file-upload__attach input[type="file"]', filePath)

                // нажать кнопку "Начать перенос почты"
                .click('.mail-import-file-upload__button_type_submit')
                .then(() => assertImportSuccess(this.browser))
                .then(() => {
                    if (fs.existsSync(filePath)) {
                        fs.unlinkSync(filePath);
                    }
                });
        });
    });

    describe('Отрицательные', () => {
        hermione.skip.in(/.*/, 'Убрали импорт из Коннекта DIR-10291');
        it('1. Некорректные данные в сервере импорта', function() {
            /* alias: neg-1-mail */
            return this.browser
                // перейти на вкладку "Mail.ru" в блоке "Выберите сервер импорта"
                .then(() => selectPreset(this.browser, 'Mail.ru', 'mail_ru'))

                // выбрать вкладку POP3 в поле "Протокол"
                .then(() => selectProtocol(this.browser, 'POP3'))

                // ввести некорректное значение в поле "Сервер", например test
                .clearInput('.mail-server-setup__server input')
                .setValue('.mail-server-setup__server input', 'test')

                // ввести некорректное значение в поле "Порт", например test
                .clearInput('.mail-server-setup__port input')
                .setValue('.mail-server-setup__port input', 'test')

                // нажать на кнопку "Дальше"
                .click('.mail-server-setup__button_type_next')

                // появилась нотификация "Не удалось соединиться с сервером импорта"
                .waitForExactText('.status-notification_type_error', 'Не удалось соединиться с сервером импорта', 15000);
        });

        hermione.skip.in(/.*/, 'Импорт не работает');
        it('2. Некорректные данные в импорте вручную', function() {
            /* alias: neg-2-manual */
            return this.browser
                // перейти на вкладку "Gmail" в блоке "Выберите сервер импорта"
                .then(() => selectPreset(this.browser, 'Gmail', 'gmail'))

                // нажать кнопку "Дальше"
                .click('.mail-server-setup__button_type_next')
                .waitForVisible('.mail-import-list', 20000)

                // ввести некорректное значение в поле "Почта", например test
                .setValue('.mail-import-list-editor__email input', 'test')

                // ввести некорректное значение в поле "Пароль", например test
                .setValue('.mail-import-list-editor__password input', 'test')

                // нажать на кнопку "Начать перенос почты"
                .click('.mail-import-list-editor__button_type_submit')

                // перенос не выполнился
                // появилось сообщение "Некорректная почта" под строчкой с введёнными данными
                .waitForExactText('.mail-import-list-editor__row_tbody:nth-child(2) .mail-import-list-editor__item-error', 'Некорректная почта', 15000)

                // ввести правильное значение в поле "Почта", например test@mail.ru
                .clearInput('.mail-import-list-editor__email input')
                .setValue('.mail-import-list-editor__email input', 'test@mail.ru')

                // нажать кнопку "Начать перенос почты"
                .click('.mail-import-list-editor__button_type_submit')

                // над кнопкой "Начать перенос почты" появилось красное сообщение об ошибке
                .waitForVisible('.mail-import-list-errors__summary-message', 30000)

                // над кнопкой "Начать перенос почты" появилась таблица с ошибками
                .waitForVisible('.mail-import-list-errors__table')

                // внешний вид вкладки ввода данных [user-input]
                .assertView('user-input', '.mail-import-list');
        });

        hermione.skip.in(/.*/, 'Импорт не работает');
        it('3. Некорректные данные в импорте через файл', function() {
            /* alias: neg-3-file */

            const brokenFilePath = join(__dirname, 'mail_import_unsuccess.csv');

            return this.browser
                // перейти на вкладку "Gmail" в блоке "Выберите сервер импорта"
                .then(() => selectPreset(this.browser, 'Gmail', 'gmail'))

                // нажимаем кнопку "Дальше"
                .click('.mail-server-setup__button_type_next')
                .waitForVisible('.mail-import-list', 20000)

                // переходим на вкладку "Из CSV-файла"
                .then(() => selectInputMode(this.browser, 'Из CSV-файла'))

                // открыта вкладка импорта из файла
                .waitForVisible('.mail-import-file-upload')

                // загрузить подготовленный некорректный файл(см description) по кнопке "Загрузить файл"
                .chooseFile('.mail-import-file-upload__attach input[type="file"]', brokenFilePath)

                // нажать кнопку "Начать перенос почты"
                .click('.mail-import-file-upload__button_type_submit')

                // над кнопкой "Начать перенос почты" появилось красное сообщение об ошибке
                .waitForVisible('.mail-import-list-errors__summary-message', 30000)

                // над кнопкой "Начать перенос почты" появилась таблица с ошибками
                .waitForVisible('.mail-import-list-errors__table')

                // внешний вид вкладки ввода данных [user-input]
                .assertView('user-input', '.mail-import-list');
        });
    });
});
