describe('Инфо', function() {
    describe('Данные', function() {
        beforeEach(function() {
            const { browser, PO } = this;

            return browser
                .yaOpenPage('/')
                .yaWaitForVisible(PO.YndxBug())
                .click(PO.YndxBug())
                .click(PO.Tabs.Info())
                .yaWaitForVisible(PO.Info());
        });

        it('В вкладке содержатся корректные данные', function() {
            const { browser, PO } = this;

            return browser
                .yaCheckText(PO.Info(), 'project : Butterfly')
                .yaAssertView('info', PO.Info.content());
        });
    });
});
