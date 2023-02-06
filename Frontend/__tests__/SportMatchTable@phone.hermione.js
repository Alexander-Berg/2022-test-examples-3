specs({
    feature: 'sportMatchTable',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        const selector = '.sport-match-table';

        return this.browser
            .url('/turbo?stub=sportmatchtable/default.json')
            .assertView('plain', selector)
            .click('.sport-match-table__tab:nth-child(2)')
            .assertView('plain-switched', selector);
    });
});
