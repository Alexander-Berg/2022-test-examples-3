specs({
    feature: 'TurboStatusScreen',
}, () => {
    hermione.only.notIn('safari13');
    it('Экран успешного оформления запроса', function() {
        return this.browser
            .url('/turbo?stub=page/nike.json&exp_flags=turboforms_endpoint=/')
            .yaIndexify(PO.oneClickForm())
            .yaIndexify(PO.oneClickForm.inputText())
            .click(PO.nikeBuyByOneClickButton())
            .yaWaitForVisible(PO.firstOneClickForm(), 'Окно оформления заказа не открылось')
            .setValue(PO.firstOneClickForm.nameField.control(), 'Роман')
            .setValue(PO.firstOneClickForm.phoneField.control(), '88001234567')
            .setValue(PO.firstOneClickForm.emailField.control(), 'test@test.ru')
            .setValue(PO.firstOneClickForm.address.control(), 'Льва Толстого, 16')
            .click(PO.firstOneClickForm.submit())
            .waitForVisible(PO.blocks.turboStatusScreenOrderSuccess())
            .assertView('success', PO.turboModal());
    });

    hermione.only.notIn('safari13');
    it('Отправка заказа со второй страницы → Экран отправки заказа', function() {
        return this.browser
            .url('/turbo?stub=page/nike.json&exp_flags=turboforms_endpoint=/')
            .yaIndexify(PO.oneClickForm())
            .yaIndexify(PO.secondOneClickForm.inputText())
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.firstRelatedAutoload(), 'Не загрузилась связанная страница')
            .click(PO.chairBuyByOneClickButton())
            .waitForVisible(PO.secondOneClickForm())
            .setValue(PO.secondOneClickForm.nameField.control(), 'Роман')
            .setValue(PO.secondOneClickForm.phoneField.control(), '88001234567')
            .setValue(PO.secondOneClickForm.emailField.control(), 'test@test.ru')
            .setValue(PO.secondOneClickForm.address.control(), 'Льва Толстого, 16')
            .click(PO.secondOneClickForm.submit())
            .waitForVisible(PO.blocks.turboStatusScreenOrderLoading())
            .assertView('loading', PO.turboModalVisible.content())
            .waitForVisible(PO.blocks.turboStatusScreenOrderSuccess());
    });

    hermione.only.notIn('safari13');
    it('Отправка заказа со второй страницы → Экран ошибки оформления заказа', function() {
        return this.browser
            .url('/turbo?stub=page/nike.json&exp_flags=turboforms_endpoint=/')
            .yaIndexify(PO.oneClickForm())
            .yaIndexify(PO.secondOneClickForm.inputText())
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.firstRelatedAutoload(), 'Не загрузилась связанная страница')
            .click(PO.chairBuyByOneClickButton())
            .waitForVisible(PO.secondOneClickForm())
            .setValue(PO.secondOneClickForm.nameField.control(), 'error')
            .setValue(PO.secondOneClickForm.phoneField.control(), '88001234567')
            .setValue(PO.secondOneClickForm.emailField.control(), 'test@test.ru')
            .setValue(PO.secondOneClickForm.address.control(), 'Льва Толстого, 16')
            .click(PO.secondOneClickForm.submit())
            .waitForVisible(PO.blocks.turboStatusScreenOrderLoading())
            .waitForVisible(PO.blocks.turboStatusScreenOrderFail())
            .assertView('fail', PO.turboModalVisible.content());
    });
});
