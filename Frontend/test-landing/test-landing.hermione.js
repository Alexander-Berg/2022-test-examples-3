const getPlatformByBrowser = require('../../../hermione/utils/get-platform-by-browser');

const BACKEND_PATH = require('./../../../configs/current/config')().backendPath;

specs({
    feature: 'Лендинги',
}, () => {
    describe('Первый лендинг', function() {
        hermione.only.notIn('safari13');
        it('Переходы между страницами', function() {
            this.browser.timeouts('script', 5000); // для стабильной работы Service Worker
            return this.browser
                .url('?text=test-landing-1&exp_flags=analytics-disabled%3D0')
                .yaStartResourceWatcher('/static/turbo/hermione/mock-external-resources.sw.js', [
                    { url: BACKEND_PATH, response: { status: 'success' } },
                ])
                .refresh()
                .yaWaitForVisible(PO.screenFirst(), 'Первая страница не показалась')
                .assertView('first-page', PO.page())
                .click(PO.screenFirst.buttonNext())
                .yaWaitForVisible(PO.screenSecond(), 'Вторая страница не показалась')
                .assertView('second-page', PO.page())
                .click(PO.screenSecond.buttonBack())
                .yaWaitForVisible(PO.screenFirst(), 'Первая страница не показалась')
                .click(PO.screenFirst.buttonNext())
                .yaWaitForVisible(PO.screenSecond(), 'Вторая страница не показалась')
                .yaWatchInnerHeight(function() {
                    return this.setValue(PO.screenSecond.inputPhone.control(), '81234567890')
                        .click(PO.screenSecond.buttonNext());
                })
                .yaWaitForVisible(PO.screenThird(), 'Третья страница не показалась')
                .assertView('third-page', PO.page())
                .yaCheckLinkOpener(PO.screenThird.buttonNext(), 'Страница не открылась в новой вкладке', { target: '_blank' })
                .then(nextUrl => {
                    assert.include(nextUrl.href, 'https://yandex.ru/', 'Неправильная ссылка "посетить сайт"');
                })
                .yaCheckExternalResourcesLoad([BACKEND_PATH])
                .then(result => this.browser.yaStopResourceWatcher()
                    .then(() => result)
                )
                .catch(err => this.browser.yaStopResourceWatcher()
                    .then(() => {
                        throw err;
                    })
                );
        });

        hermione.only.notIn('safari13');
        it('Пользовательское соглашение', function() {
            return this.browser
                .url('?text=test-landing-1&exp_flags=analytics-disabled%3D0')
                .yaWaitForVisible(PO.screenFirst(), 'Первая страница не показалась')
                .click(PO.screenFirst.buttonNext())
                .yaWaitForVisible(PO.screenSecond(), 'Вторая страница не показалась')
                .yaCheckLinkOpener(PO.screenSecond.agreement.link(), 'Ссылка "пользовательское соглашение" не открылась в новой вкладке', { target: '_blank' })
                .then(agreementUrl => {
                    assert.include(agreementUrl.href, 'https://yandex.ru/legal/rules/', 'Неправильная ссылка "пользовательское соглашение"');
                })
                .click(PO.screenSecond.agreement.fullHandler())
                .yaWaitForVisible(PO.modal(), 'Модальное окно не показалось')
                .assertView('second-page-modal-agreement', PO.modal())
                .click(PO.modal.buttonClose())
                .yaWaitForHidden(PO.modal(), 'Модальное окно не скрылось');
        });

        hermione.only.notIn('safari13');
        it('Страница ввода значений', function() {
            return this.browser
                .url('?text=test-landing-1&exp_flags=analytics-disabled%3D0')
                .yaWaitForVisible(PO.screenFirst(), 'Первая страница не показалась')
                .click(PO.screenFirst.buttonNext())
                .yaWaitForVisible(PO.screenSecond(), 'Вторая страница не показалась')
                .getValue(PO.screenSecond.inputPhone.control())
                .then(value => assert.equal('', value, 'Поле ввода не пустое'))
                .yaWaitForHidden(PO.screenSecond.inputPhone.clear(), 'Не скрылся "крестик"')
                .click(PO.screenSecond.inputPhone.control())
                .setValue(PO.screenSecond.inputPhone.control(), '81234567890')
                .getValue(PO.screenSecond.inputPhone.control())
                .then(value => assert.equal('81234567890', value, 'Поле ввода не заполнено'))
                .yaWaitForVisible(PO.screenSecond.inputPhone.clear(), 'Не показался "крестик"')
                .click(PO.screenSecond.inputPhone.clear())
                .getValue(PO.screenSecond.inputPhone.control())
                .then(value => assert.equal('', value, 'Не пустое поле ввода'))
                .setValue(PO.screenSecond.inputPhone.control(), '81234567890')
                .getValue(PO.screenSecond.inputPhone.control())
                .then(value => assert.equal('81234567890', value, 'Поле ввода не заполнено'))
                .click(PO.screenSecond.selectCity.control())
                .selectByValue(PO.screenSecond.selectCity.control(), 'Казань')
                .getValue(PO.screenSecond.selectCity.control())
                .then(value => assert.equal('Казань', value, 'Не выбрано значение "Казань"'))
                .getValue(PO.screenSecond.textareaComment.control())
                .then(value => assert.equal('', value, 'Поле ввода не пустое'))
                .yaWaitForHidden(PO.screenSecond.textareaComment.clear(), 'Не скрылся "крестик"')
                .click(PO.screenSecond.textareaComment.control())
                .setValue(PO.screenSecond.textareaComment.control(), 'мой вопрос')
                .getValue(PO.screenSecond.textareaComment.control())
                .then(value => assert.equal('мой вопрос', value, 'Поле ввода не заполнено'))
                .yaWaitForVisible(PO.screenSecond.textareaComment.clear(), 'Не показался "крестик"')
                .click(PO.screenSecond.textareaComment.clear())
                .getValue(PO.screenSecond.textareaComment.control())
                .then(value => assert.equal('', value, 'Не пустое поле ввода'))
                .setValue(PO.screenSecond.textareaComment.control(), 'мой вопрос')
                .getValue(PO.screenSecond.textareaComment.control())
                .then(value => assert.equal('мой вопрос', value, 'Поле ввода не заполнено'))
                .yaWaitForHidden(PO.screenThird(), 'Третья страница показалась');
        });
    });

    describe('Второй лендинг', function() {
        hermione.only.notIn('safari13');
        it('Валидация полей', function() {
            this.browser.timeouts('script', 5000); // для стабильной работы Service Worker
            return this.browser
                .url('?text=test-landing-2&exp_flags=analytics-disabled%3D0')
                .yaStartResourceWatcher('/static/turbo/hermione/mock-external-resources.sw.js', [
                    { url: BACKEND_PATH, response: { status: 'success' } },
                ])
                .refresh()
                .yaWaitForVisible(PO.screenFirst(), 'Первая страница не показалась')
                .click(PO.screenFirst.buttonNext())
                .yaWaitForVisible(PO.screenSecond(), 'Вторая страница не показалась')
                .click(PO.screenSecond.buttonNext())
                .yaWaitForVisible(PO.screenSecond.inputPhoneError(), 'Не появилась подсветка ошибки у поля телефон')
                .yaWaitForVisible(PO.screenSecond.inputEmailError(), 'Не появилась подсветка ошибки у поля email')
                .setValue(PO.screenSecond.inputPhone.control(), 'aaaaa')
                .yaWaitForVisible(PO.screenSecond.inputPhoneError(), 'Не появилась подсветка ошибки у поля телефон')
                .getText(PO.screenSecond.inputPhoneErrorText()).then(function(text) {
                    assert.equal(text, 'Номер телефона указан неверно', 'Неправильный текст ошибки');
                })
                .setValue(PO.screenSecond.inputPhone.control(), '81234567890')
                .yaWaitForHidden(PO.screenSecond.inputPhoneErrorText(), 'Не скрылся текст ошибки у поля телефон')
                .yaWaitForHidden(PO.screenSecond.inputPhoneError(), 'Не скрылась подсветка ошибки у поля телефон')
                .setValue(PO.screenSecond.inputEmail.control(), '11111')
                .yaWaitForVisible(PO.screenSecond.inputEmailError(), 'Не появилась подсветка ошибки у поля email')
                .getText(PO.screenSecond.inputEmailErrorText()).then(function(text) {
                    assert.equal(text, 'Email указан неверно', 'Неправильный текст ошибки у поля email');
                })
                .setValue(PO.screenSecond.inputEmail.control(), 'test@example.com')
                .yaWaitForHidden(PO.screenSecond.inputEmailErrorText(), 'Не скрылся текст ошибки у поля email')
                .yaWaitForHidden(PO.screenSecond.inputEmailError(), 'Не скрылась подсветка ошибки у поля email')
                .click(PO.screenSecond.buttonNext())
                .yaCheckExternalResourcesLoad([BACKEND_PATH])
                .then(result => this.browser.yaStopResourceWatcher()
                    .then(() => result)
                )
                .catch(err => this.browser.yaStopResourceWatcher()
                    .then(() => {
                        throw err;
                    })
                )
                .yaWaitForVisible(PO.screenThird(), 'Третья страница не показалась');
        });

        hermione.only.notIn('safari13');
        it('Плавающая кнопка', function() {
            return this.browser
                .url('?text=test-landing-2&exp_flags=analytics-disabled%3D0')
                .yaWaitForVisible(PO.screenFirst(), 'Первая страница не показалась')
                .yaScrollPage(PO.screenFirst.paragraph())
                .yaWaitForVisible(PO.screenFirst.stickyFixed(), 'Стики-кнопка не "плавает"')
                .yaScrollPage(PO.screenFirst.footer())
                .yaWaitForVisible(PO.screenFirst.stickyBottom(), 'Стики-кнопка не зафиксирована внизу страницы');
        });

        hermione.only.notIn('safari13');
        it('Проверка счетчиков', function() {
            return this.browser
                .url('?text=test-landing-2&exp_flags=analytics-disabled%3D0')
                .yaWaitForVisible(PO.screenFirst(), 'Первая страница не показалась')
                .yaTouchScroll(PO.screenFirst.footer())
                .click(PO.screenFirst.buttonNext())
                .yaWaitForVisible(PO.screenSecond(), 'Вторая страница не показалась')
                .click(PO.screenSecond.inputPhone.control());
        });

        hermione.only.notIn('safari13');
        it('Проверка счетчиков метрики', function() {
            const expectedParams = {
                pageId: 'test-landing-2',
                __ym: { turbo_page: 1, doc_ui: getPlatformByBrowser(hermione, this.browser), turbo_page_id: 'test-landing-2', domain_type: 'yandex' },
            };

            return this.browser
                .url('?text=test-landing-2&exp_flags=analytics-disabled=0')
                .yaWaitForVisible('.metrika_loaded', 'Метрика не заинитилась')
                .yaWaitForVisible(PO.screenFirst(), 'Первая страница не показалась')
                .click(PO.screenFirst.buttonNext())
                .yaWaitForVisible(PO.screenSecond(), 'Вторая страница не показалась')
                .execute(function() {
                    return window.Ya.Metrika.getGoalsFor(46417413, 1);
                })
                .then(({ value: goals }) => {
                    assert.deepEqual(goals,
                        [
                            ['g1', expectedParams],
                            ['g2', expectedParams],
                        ]
                    );
                });
        });

        hermione.only.notIn('safari13');
        it('Реальный лендинг', function() {
            return this.browser
                .url('?text=lpc%2Ff1631fc7b21f1a5ca1ca34c6e1d3c4c8236174737e35b473303b089cbd2556ec&yclid=4498062521165450332')
                .assertView('plain', 'html');
        });
    });
});
