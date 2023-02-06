specs({
    feature: 'sportRaceTeaser',
}, () => {
    it('Внешний вид блока', function() {
        const selector = '.sport-race-teaser';

        return this.browser
            .url('/turbo?stub=sportraceteaser/default.json')
            .assertView('plain', selector);
    });
});
