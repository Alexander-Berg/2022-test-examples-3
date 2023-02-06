specs({
    feature: 'Страница тачевого спортивного календаря',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        const selector = '.sport-match-center-page';

        return this.browser
            .url('/turbo?stub=sportmatchcenterpage/default.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });
});
