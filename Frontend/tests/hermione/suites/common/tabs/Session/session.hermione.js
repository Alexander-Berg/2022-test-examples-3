describe('Сессия', function() {
    describe('Работа с сессией', function() {
        beforeEach(function() {
            const { browser, PO } = this;

            return browser
                .yaOpenPage('/')
                .yaWaitForVisible(PO.YndxBug())
                .click(PO.YndxBug())
                .click(PO.Tabs.Session())
                .yaWaitForVisible(PO.Session());
        });

        it('При нажатии на кнопку "начать" можно ввести имя в поле ввода', function() {
            const { browser, PO } = this;

            return browser
                .yaAssertView('sessionWithoutInput', PO.Session.ControlBar())
                .yaShouldNotBeVisible(PO.Session.Input())
                .click(PO.Session.StartBtn())
                .yaShouldBeVisible(PO.Session.Input())
                .setValue(PO.Session.Input(), 'Моя сессия')
                .yaCheckValue(PO.Session.Input(), 'Моя сессия')
                .yaAssertView('sessionWithInput', PO.Session.ControlBar());
        });

        it('Можно начать сессию не вводя ничего в поле ввода', function() {
            const { browser, PO } = this;

            return browser
                .yaShouldNotBeVisible(PO.Session.Info.Status())
                .click(PO.Session.StartBtn())
                .click(PO.Session.StartBtn())
                .yaWaitForVisible(PO.Session.Info.Status())
                .yaCheckText(PO.Session.Info.Status(), 'Статус: recording');
        });

        it('Введенное имя отобразится в информации о сессии', function() {
            const { browser, PO } = this;

            return browser
                .click(PO.Session.StartBtn())
                .setValue(PO.Session.Input(), 'Моя сессия')
                .click(PO.Session.StartBtn())
                .yaWaitForVisible(PO.Session.Name())
                .yaCheckText(PO.Session.Name(), 'Моя сессия');
        });

        it('Можно начать сессию введя имя сессии', function() {
            const { browser, PO } = this;

            return browser
                .yaShouldNotBeVisible(PO.Session.Info.Status())
                .click(PO.Session.StartBtn())
                .setValue(PO.Session.Input(), 'Моя сессия')
                .click(PO.Session.StartBtn())
                .yaWaitForVisible(PO.Session.Info.Status())
                .yaCheckText(PO.Session.Info.Status(), 'Статус: recording')
                .yaCheckText(PO.Session.Name(), 'Моя сессия');
        });

        it('По окончании сессии появится ссылка для просмотра сессии', function() {
            const { browser, PO } = this;

            return browser
                .click(PO.Session.StartBtn())
                .pause(500)
                .click(PO.Session.StartBtn())
                .pause(500)
                .yaShouldNotBeVisible(PO.Session.Info.Link())
                .click(PO.Session.StartBtn())
                .pause(500)
                .yaWaitForVisible(PO.Session.Info.Link());
        });

        it('При перезагрузке страницы статус обновился', function() {
            const { browser, PO } = this;

            return browser
                .click(PO.Session.StartBtn())
                .setValue(PO.Session.Input(), 'Моя сессия')
                .click(PO.Session.StartBtn())
                .yaOpenPage('/')
                .yaWaitForVisible(PO.YndxBug())
                .click(PO.YndxBug())
                .click(PO.Tabs.Session())
                .yaWaitForVisible(PO.Session.Info.Status())
                .yaCheckText(PO.Session.Info.Status(), 'Статус: recording')
                .yaCheckText(PO.Session.Name(), 'Моя сессия');
        });
    });
});
