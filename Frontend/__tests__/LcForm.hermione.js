specs({
    feature: 'LcForm',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lcform/lcformtests.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcForm(), 'Форма не появилась')
            .assertView('plain', PO.lcForm());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока c красным цветом текста', function() {
        return this.browser
            .url('/turbo?stub=lcform/with-text-color.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcForm(), 'Форма не появилась')
            .assertView('plain', PO.lcForm());
    });

    hermione.only.notIn('safari13');
    it('Должна убирать ошибку при начале печатания', function() {
        return this.browser
            .url('/turbo?stub=lcform/lcformtests.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcForm(), 'Форма не появилась')
            .click(PO.lcForm.submit())
            .pause(300)
            .yaWaitForVisible('.lc-input_type_tel.lc-input_invalid', 'Ошибка не появилась')
            .click(PO.lcInput.phone.input())
            .keys('a')
            .yaWaitForHidden('.lc-input_type_tel.lc-input_invalid', 'Ошибка не исчезла');
    });

    hermione.only.notIn('safari13');
    it('Должно не пропускать невалидную форму', function() {
        return this.browser
            .url('/turbo?stub=lcform/lcformtests.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcForm(), 'Форма не появилась')
            .click(PO.lcInput.email.input())
            .keys('abrakadabra'.split(''))
            .pause(300)
            .click(PO.lcForm.submit())
            .yaWaitForHidden(PO.lcFormModal.container(), 'Появилось модальное окно при невалидной форме')
            .assertView('form-invalid', PO.lcForm());
    });

    hermione.only.notIn('safari13');
    it('Не должна пропускать пустую форму', function() {
        return this.browser
            .url('/turbo?stub=lcform/lcformtests.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcForm(), 'Форма не появилась')
            .click(PO.lcForm.submit())
            .yaWaitForHidden(PO.lcFormModal.container(), 'Появилось модальное окно при пустой форме')
            .assertView('form-empty', PO.lcForm());
    });

    const runSubmitTest = (select, optionSelect, platformName) => {
        hermione.only.notIn('safari13');
        it(`Должна отправляться и показывать модал благодарности у ${platformName}`, function() {
            return this.browser
                .url('/turbo?stub=lcform/lcformtests.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .yaWaitForVisible(PO.lcForm(), 'Форма не появилась')
                .click(PO.lcInput.phone.input())
                .keys('79261234567'.split(''))
                .click(PO.lcInput.email.input())
                .keys('email@gmail.com'.split(''))
                .click(select)
                .pause(300)
                .click(optionSelect)
                .assertView(`filled-form-${platformName}`, PO.lcForm())
                .click(PO.lcForm.submit())
                .yaWaitForVisible(PO.lcFormModal.container(), 'Окно благодарности не появилось')
                .assertView('form-submitted-modal', PO.lcFormModal.container())
                .click(PO.lcFormModal.close())
                .yaWaitForHidden(PO.lcFormModal.container(), 'Окно благодарности не скрылось');
        });
    };

    hermione.only.in(['chrome-phone', 'iphone', 'searchapp']);
    runSubmitTest(PO.lcSelect.mobile(), PO.lcSelect.secondOptionMobile(), 'mobile');
    hermione.only.in(['chrome-desktop', 'firefox']);
    runSubmitTest(PO.lcSelect.desktop(), PO.lcSelect.secondOption(), 'desktop');

    hermione.only.notIn('safari13');
    it('Форма автозаполняется данными из паспорта', function() {
        return this.browser
            .url('/turbo?stub=lcform/fortest.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcForm(), 'Форма не появилась')
            .assertView('plain', PO.lcForm());
    });

    hermione.only.notIn('safari13');
    it('Ошибка и саджест пропадают после клика на саджест', function() {
        return this.browser
            .url('/turbo?stub=lcform/fortest.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcForm(), 'Форма не появилась')
            .click(PO.lcInput.phone.clear())
            .click(PO.lcForm.suggest())
            .yaWaitForHidden(PO.lcForm.error(), 'Ошибка не пропала')
            .assertView('view', PO.lcForm());
    });

    hermione.only.notIn('safari13');
    it('Перенос длинных заголовков полей формы на новую строку', function() {
        return this.browser
            .url('/turbo?stub=lcform/with-long-input-label.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcForm());
    });

    hermione.only.notIn('safari13');
    it('Должны очищаться инпуты после успешной отправки', function() {
        return this.browser
            .url('/turbo?stub=lcform/short.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcForm(), 'Форма не появилась')
            .yaMockFetch({
                status: 200,
                urlDataMap: {
                    '/lcForm': '{}',
                },
            })
            .click(PO.lcInput.phone.input())
            .keys('79261234567'.split(''))
            .click(PO.lcInput.email.input())
            .keys('email@gmail.com'.split(''))
            .click(PO.lcForm.submit())
            .yaWaitForVisible(PO.lcFormModal.container(), 'Окно благодарности не появилось')
            .click(PO.lcFormModal.close())
            .yaWaitForHidden(PO.lcFormModal.container(), 'Окно благодарности не скрылось')
            .assertView('cleared-form', PO.lcForm());
    });
});
