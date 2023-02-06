hermione.only.in('chrome-phone');

specs({
    feature: 'Спортивный календарь',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        const selector = '.sport-calendar';

        return this.browser
            .url('/turbo?stub=sportcalendar/default.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });
});
