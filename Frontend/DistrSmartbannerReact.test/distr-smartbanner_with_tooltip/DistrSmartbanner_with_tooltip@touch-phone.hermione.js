'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn(['searchapp-phone', 'chrome-phone'], 'не браузерозависимо, в ПП нет смартбаннеров');
specs({
    feature: 'Смартбаннер React',
    type: 'with-tooltip',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'asdfghjklasdfghasdfghjklasdfgh',
            foreverdata: 3769250375,
            exp_flags: ['smartbanner_atom=1'],
        }, PO.serpList());
    });

    it('Смартбаннер находится над тултипом verified', async function() {
        const PO = this.PO;

        // Иначе шапка вызывает закрытие попапов
        // Раздебажить не удалось
        await this.browser.yaHideHeader();

        await this.browser.yaRetryActionsByElemVisible(
            5,
            PO.verifiedTooltip(),
            () => this.browser.yaTouch(PO.verifiedIcon()),
        );

        await this.browser.assertView('plain', PO.serpItemSecond());
    });
});
