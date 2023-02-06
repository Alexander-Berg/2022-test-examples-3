describe('Dropdown', () => {
    describe('открытие попапа', () => {
        it('показывает попап при наведении', function() {
            return this.browser
                .url('Dropdown/hermione/hermione.html')
                .moveToObject('.Hermione-CursorReset')
                .moveToObject('.hover .Button2')
                .pause(200)
                .assertView('hovered', '.hover')
                .moveToObject('.Hermione-CursorReset')
                .pause(200)
                .assertView('unhovered', '.hover');
        });

        it('показывает попап по клику', function() {
            return this.browser
                .url('Dropdown/hermione/hermione.html')
                .click('.click .Button2')
                .assertView('clicked', '.click')
                .click('.click .Button2')
                .assertView('2nd click', '.click');
        });

        it('показывает попап по фокусу', function() {
            return this.browser
                .url('Dropdown/hermione/hermione.html')
                .click('.focus .Button2')
                .assertView('focus', '.focus')
                .click('.focus')
                .assertView('blur', '.click');
        });
    });
});
