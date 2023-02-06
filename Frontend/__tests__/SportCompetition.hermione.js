specs({
    feature: 'Спортивное соревнование',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        const selector = '.sport-competition';

        return this.browser
            .url('/turbo?stub=sportcompetition/default.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });

    hermione.only.notIn('safari13');
    it('Базовый вид блока со стадией', function() {
        const selector = '.sport-competition';

        return this.browser
            .url('/turbo?stub=sportcompetition/with-phase.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('with-phase', selector);
    });

    hermione.only.in(['chrome-desktop', 'chrome-phone', 'iphone', 'searchapp']);
    hermione.only.notIn('safari13');
    it('Блок скрывается при клике по заголовку', function() {
        const selector = '.sport-competition';

        return this.browser
            .url('/turbo?stub=sportcompetition/toggleable.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .click(`${selector}__icon-wrapper`)
            .yaWaitForVisible(`${selector}__toggler:not(.news-cut_expanded)`, 'Блок не свернулся')
            .assertView('toggleable-close', selector);
    });

    hermione.only.notIn('safari13');
    it('Блок без команд', function() {
        const selector = '.sport-competition';

        return this.browser
            .url('/turbo?stub=sportcompetition/no-team.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('without-team', selector);
    });
});
