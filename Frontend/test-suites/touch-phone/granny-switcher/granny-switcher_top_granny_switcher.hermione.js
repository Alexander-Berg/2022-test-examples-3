'use strict';

const PO = require('../../../page-objects/touch-phone').PO;

specs({
    feature: 'Cообщение о медленном интернете',
    experiment: 'Новая плашка про медленный интернет'
}, function() {
    hermione.only.notIn('winphone', 'winphone не touch, а smart, там нет переключения из медленного интернета');
    it('Обязательные проверки', function() {
        return this.browser
            .yaOpenSerp({
                text: 'быбуля',
                exp_flags: 'top_granny_switcher',
                user_connection: 'slow_connection=1'
            })
            .yaWaitForVisible(
                PO.grannySwitcher(),
                'Не появилось сообщение о медленном интернете'
            )
            .assertView('plain', PO.grannySwitcher())
            .yaCheckLink(PO.grannySwitcher.toggle(), { target: '' })
            .yaCheckBaobabCounter(PO.grannySwitcher.toggle(), {
                path: '/$page/$main/granny-switcher/link[@type="switcher"]'
            });
    });
});
