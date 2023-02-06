specs({
    feature: 'Матч-центр (гонка)',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        const selector = '.sport-match-center-race';

        return this.browser
            .url('/turbo?stub=sportmatchcenterrace/default.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });
});
