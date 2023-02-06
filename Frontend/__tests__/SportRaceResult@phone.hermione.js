specs({
    feature: 'sportRaceResult',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        const selector = '.sport-race-result';

        return this.browser
            .url('/turbo?stub=sportraceresult/default.json')
            .assertView('plain', selector)
            .yaTouchScroll('.sport-race-result__right-part', 1000, 0)
            .assertView('plain-scroll', selector);
    });
});
