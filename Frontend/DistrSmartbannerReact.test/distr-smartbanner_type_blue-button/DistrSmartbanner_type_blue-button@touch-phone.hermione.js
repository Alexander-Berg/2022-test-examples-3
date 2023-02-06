'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn('searchapp-phone'); // смартбаннера нет в поисковом приложении
specs({ feature: 'Смартбаннер React', type: 'blue-button' }, function() {
    hermione.also.in('iphone-dark');
    it('Внешний вид и закрытие смартбаннера', async function() {
        await this.browser.yaOpenSerp({
            text: 'цветы доставка',
            foreverdata: 1544808643,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-blue-button', PO.smartInfo());

        await this.browser.yaCheckBaobabCounter(PO.smartInfo.close(), {
            path: '/$page/distr-smartbanner/close[@tags@close=1 and @behaviour@type="dynamic" and @layout="blue-button"]',
        });
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Смартбаннер ЯБро не закрылся');
    });

    it('Клик в ссылку в смартбаннере', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 1544808643,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaCheckBaobabCounter(PO.smartInfo.action(), {
            path: '/$page/distr-smartbanner/ok[@layout="blue-button"]',
        });
    });
});
