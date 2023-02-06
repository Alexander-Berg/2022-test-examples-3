specs({
    feature: 'reactionsControl',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока без реакций', function() {
        return this.browser
            .url('/turbo?stub=reactionscontrol/empty.json')
            .yaWaitForVisible(PO.reactionsControl())
            .assertView('plain', PO.reactionsControl())
            .click(PO.reactionsControl.control())
            .yaWaitForVisible(PO.reactionsControlPopup())
            .assertView('popup', PO.reactionsControlPopup(), { allowViewportOverflow: true })
            .click(PO.page())
            .yaShouldNotBeVisible(PO.reactionsControlPopup(), 'Попап не скрылся после клика на пустом месте');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока из топ-3 c реакцией пользователя', function() {
        return this.browser
            .url('/turbo?stub=reactionscontrol/top-3-user.json')
            .yaWaitForVisible(PO.reactionsControl())
            .assertView('plain', PO.reactionsControl());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока из топ-3 без реакции пользователя', function() {
        return this.browser
            .url('/turbo?stub=reactionscontrol/top-3.json')
            .yaWaitForVisible(PO.reactionsControl())
            .assertView('plain', PO.reactionsControl());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока из топ-2 с реакцией пользователя', function() {
        return this.browser
            .url('/turbo?stub=reactionscontrol/top-2-user.json')
            .yaWaitForVisible(PO.reactionsControl())
            .assertView('plain', PO.reactionsControl());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока из топ-2 без реакции пользователя', function() {
        return this.browser
            .url('/turbo?stub=reactionscontrol/top-2.json')
            .yaWaitForVisible(PO.reactionsControl())
            .assertView('plain', PO.reactionsControl());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока из топ-1 с реакцией пользователя', function() {
        return this.browser
            .url('/turbo?stub=reactionscontrol/top-1-user.json')
            .yaWaitForVisible(PO.reactionsControl())
            .assertView('plain', PO.reactionsControl());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока из топ-1 без реакции пользователя', function() {
        return this.browser
            .url('/turbo?stub=reactionscontrol/top-1.json')
            .yaWaitForVisible(PO.reactionsControl())
            .assertView('plain', PO.reactionsControl());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с единственной реакцией пользователя', function() {
        return this.browser
            .url('/turbo?stub=reactionscontrol/user-only.json')
            .yaWaitForVisible(PO.reactionsControl())
            .assertView('plain', PO.reactionsControl());
    });
});
