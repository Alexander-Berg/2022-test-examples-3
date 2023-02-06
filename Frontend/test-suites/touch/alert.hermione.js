describe('alert', function() {
    it('arrow', function() {
        return this.browser
            .yaOpenExample('home', 'touch')
            .yaMockSuggest('')
            .click('.mini-suggest__input')
            .click('.mini-suggest__button')
            .assertView('alert', 'body')
            .click('.mini-suggest__alert')
            .assertView('alert-click', 'body');
    });
});
