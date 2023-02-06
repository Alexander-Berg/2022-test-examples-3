specs({
    feature: 'Страница турнира',
}, () => {
    it('Базовый вид блока', function() {
        const selector = '.sport-competition-container';

        return this.browser
            .url('/turbo?stub=sportcompetitioncontainer/default-desktop.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector)
            .click('.health-select')
            .yaWaitForVisible('.health-select__dropdown', 'Блок не появился')
            .assertView('select', selector);
    });
});
