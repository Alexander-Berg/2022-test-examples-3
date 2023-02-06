specs({
    feature: 'Текстовая трансляция',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        const selector = '.sport-match-text-live';

        return this.browser
            .url('/turbo?stub=sportmatchtextlive/default.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });
});
