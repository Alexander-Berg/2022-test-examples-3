specs({
    feature: 'Страница турнира',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        const selector = '.sport-competition-container';

        return this.browser
            .url('/turbo?stub=sportcompetitioncontainer/default-phone.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });
});
