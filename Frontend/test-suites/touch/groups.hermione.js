describe('groups', function() {
    it('groups', function() {
        return this.browser
            .yaOpenExample('serp', 'touch')
            .click('.mini-suggest__input:not(.mini-suggest__placeholder)')
            .yaMockSuggest('миронов', require('./mocks-group/homonomy.json'))
            .keys('миронов')
            .assertView('simple', 'body');
    });
});
