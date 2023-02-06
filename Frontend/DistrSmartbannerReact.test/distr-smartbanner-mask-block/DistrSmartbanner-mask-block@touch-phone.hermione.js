'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn('searchapp-phone', 'смартбаннера нет в поисковом приложении');
specs({
    feature: 'Смартбаннер React',
    type: 'В режиме маскировки',
    experiment: 'Скрываем смарт-баннер от блокировщика генерируя рандомное имя для блока',
}, function() {
    it('В режиме маскировки на выдаче блока с классом \'DistrSmartbanner\' не должно быть', async function() {
        await this.browser.deleteCookie();

        await this.browser.yaOpenSerp({
            text: 'cats',
            foreverdata: 1239240421,

            exp_flags: [
                'hidedist=1',
                'hideads=1',
                'smartbanner_atom=1',
            ],

            data_filter: false,
        }, PO.page());

        await this.browser.yaShouldNotExist(PO.smartInfo());
    });

    it('С выключенным режимом маскировки на выдаче должен быть СБ с классом \'DistrSmartbanner\'', async function() {
        await this.browser.deleteCookie();

        await this.browser.yaOpenSerp({
            text: 'cats',
            foreverdata: 1239240421,

            exp_flags: [
                'hidedist=0',
                'hideads=1',
                'smartbanner_atom=1',
            ],

            data_filter: false,
        }, PO.smartInfo());
    });
});
