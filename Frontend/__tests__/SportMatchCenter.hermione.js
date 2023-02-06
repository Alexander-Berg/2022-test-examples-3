hermione.only.in('chrome-phone');

specs({
    feature: 'Матч-центр',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        const selector = '.sport-match-center';

        return this.browser
            .url('/turbo?stub=sportmatchcenter/default.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });

    hermione.only.notIn('safari13');
    it('Базовый вид блока без текущих матчей', function() {
        const selector = '.sport-match-center';

        return this.browser
            .url('/turbo?stub=sportmatchcenter/without-current.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });

    hermione.only.notIn('safari13');
    it('Базовый вид блока с завтрашними матчами', function() {
        const selector = '.sport-match-center';

        return this.browser
            .url(`/turbo?stub=sportmatchcenter/with-tomorrow.json&user_time=${encodeURIComponent('2019-10-18T12:00:00+0300')}`)
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });
});
