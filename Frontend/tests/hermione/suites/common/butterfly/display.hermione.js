describe('Жучок', function() {
    describe('Отображение', function() {
        beforeEach(function() {
            const { browser, PO } = this;

            return browser
                .yaOpenPage('/')
                .yaWaitForVisible(PO.YndxBug())
                .click(PO.YndxBug());
        });

        it('Жучок открыт', function() {
            const { browser, PO } = this;

            return browser
                .yaShouldBeVisible(PO.YndxBug.Bug())
                .yaAssertView('bug', PO.YndxBug.Bug());
        });

        it('Жучок сохраняет состояние при сворачивании', function() {
            const { browser, PO } = this;

            return browser
                .click(PO.YndxBug.HideBtn())
                .yaShouldExist(PO.YndxBug.Bug())
                .click(PO.YndxBug());
        });

        it('Жучок закрыт', function() {
            const { browser, PO } = this;

            return browser
                .yaShouldBeVisible(PO.YndxBug.Bug())
                .click(PO.YndxBug.CloseBtn())
                .yaWaitForVisible(PO.YndxBug.Icon())
                .yaShouldNotBeVisible(PO.YndxBug.Bug())
                .yaAssertView('icon', PO.YndxBug());
        });
    });
});
