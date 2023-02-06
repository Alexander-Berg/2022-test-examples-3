describe('Form2', function() {
    hermione.only.notIn('safari13');
    it('Видимость полей по условию', function() {
        return this.browser
            .url('/turbo?stub=form2/with-visibility.json')
            .yaWaitForVisible('.turbo-form2', 'На странице нет формы')
            .yaIndexify('.radio-item')
            .yaShouldBeVisible('textarea', 'Изначально не видно поле адреса')
            .yaShouldNotBeVisible('.turbo-input-text__control', 'Изначально видно инпут')
            .click('[data-index="2"]')
            .setValue('textarea', 'test')
            .yaShouldBeVisible('.turbo-input-text__control', 'Инпут не показался после')
            .click('[data-index="3"]')
            .yaShouldNotBeVisible('textarea', 'Поле адреса не скрылось')
            .yaShouldNotBeVisible('.turbo-input-text__control', 'Инпут не скрылся')
            .click('[data-index="2"]')
            .yaShouldBeVisible('textarea', 'После возвращения способа доставки не виден адрес')
            .yaShouldBeVisible('.turbo-input-text__control', 'После возвращения способа доставки не виден input')
            .getValue('textarea')
            .then(value => assert.equal(value, 'test'))
            .setValue('textarea', 'testt')
            .yaShouldNotBeVisible('.turbo-input-text__control', 'После изменения адреса виден инпут');
    });

    hermione.only.notIn('safari13');
    describe('Динамическая форма', function() {
        it('Успешный ответ', function() {
            return this.browser
                .url('/turbo?stub=form2/dynamic.json')
                .yaWaitForVisible(PO.turboForm2(), 'На странице нет формы')
                .yaScrollPageToBottom()
                .click(PO.turboForm2.submit())
                .yaWaitForVisible(PO.turboFormResult(), 'Блок с результатом не появился')
                .yaScrollPageToBottom()
                .assertView('result', PO.turboFormResult());
        });

        it('Две формы на странице отправляют данные', function() {
            return this.browser
                .url('/turbo?stub=form2/dynamic-two-forms.json')
                .execute(function() {
                    const originalFetch = window.fetch;

                    window.fetch = function(url, options) {
                        window.__requests = window.__requests || [];
                        if (url.indexOf('/ajax/data-bind') > -1) {
                            window.__requests.push(options);
                        }

                        return originalFetch.apply(this, arguments);
                    };
                })
                .yaIndexify(PO.turboForm2())
                .yaScrollPage(PO.turboForm2First.submit())
                .click(PO.turboForm2First.submit())
                .yaScrollPage(PO.turboForm2Second.submit())
                .click(PO.turboForm2Second.submit())
                .execute(function() {
                    return window.__requests;
                })
                .then(({ value }) => {
                    const [firstReq, secondReq] = value;
                    assert.equal(firstReq.body, '{"address":"Льва Толстого, 16","name":"Тест"}', 'Тело запроса первой формы некорректное');
                    assert.equal(secondReq.body, '{"address":"Льва Толстого, 16","name":"Тест23"}', 'Тело запроса первой формы некорректное');
                });
        });

        it('Ошибка', function() {
            return this.browser
                .url('/turbo?stub=form2/dynamic-error.json')
                .yaWaitForVisible(PO.turboForm2(), 'На странице нет формы')
                .click(PO.turboForm2.submit())
                .yaWaitForVisible(PO.turboAlert(), 'Сообщение об ошибке не появилось')
                .getText(PO.turboAlert.text())
                .then(text => {
                    assert.strictEqual(text, 'Произошла ошибка', 'Сообщение об ошибке содержит некорректный текст');
                });
        });
    });
});
