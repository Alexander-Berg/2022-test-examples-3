describe('static', function() {
    it('arrow', function() {
        return this.browser
            .yaOpenExample('home', 'touch')
            .waitForExist('.i-ua_js_yes')
            .assertView('plain', '.search')
            .click('.search')
            .assertView('focused', '.search')
            .yaMouseDown('.mini-suggest__button')
            .assertView('pressed', '.search');
    });

    it('input', function() {
        return this.browser
            .yaOpenExample('home', 'touch')
            .execute(function() {
                document.documentElement.className = 'i-ua_js_yes i-ua_inlinesvg_no';
            })
            .click('.search')
            .keys('value')
            .assertView('filled', '.search')
            .execute(function() {
                var clear = document.querySelector('.mini-suggest__input-clear'),
                    url = getComputedStyle(clear).backgroundImage;

                if (url.indexOf('.svg') > -1) {
                    clear.style.background = 'transparent';
                }
            })
            .assertView('clear-png', '.search');
    });
});
