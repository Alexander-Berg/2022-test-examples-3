const PO = require('./PO');

describe('Вложенные сервисы', function() {
    describe('Положительные', function() {
        it('1. Отображение вложенных сервисов внутри конкретного сервиса для ограниченной роли', async function() {
            // открыть сервис "autotest-nested-services" на вкладке "Вложенные сервисы"
            await this.browser.openIntranetPage({
                pathname: '/services/autotest-nested-services/services/',
            }, {
                user: 'robot-abc-003'
            });
            // страница открылась
            await this.browser.waitForVisible(PO.tree.services(), 7000);
            // в фильтрах поставить галочку напротив пункта "С внешними"
            await this.browser.click(PO.filters.externals.withExternalsCheckbox.input());
            // список сервисов обновляется - крутится спиннер
            await this.browser.waitForVisible(PO.spinner(), 5000);
            await this.browser.waitForVisible(PO.spinner(), 10000, true);
            // над списком сервисов добавился выбранный фильтр "С внешними"
            await this.browser.waitForVisible(PO.summary.withExternalsLabel(), 500);
            // в URL появился параметр "hasExternalMembers=true"
            await this.browser.yaAssertUrlParam('hasExternalMembers', 'true');
            // фоткаем результат фильтрации
            await this.browser.assertView('nested-services', PO.tree.services());
            // кликнуть на значок ">" рядом с названием сервиса "autotest-descendant-1"
            await this.browser.click(PO.tree.services.firstService.openTreeButton());
            // появились вложенные для "autotest-descendant-1" сервисы
            await this.browser.waitForVisible(PO.tree.services.firstAutotestChild(), 5000);
            // фоткаем развёрнутое дерево
            await this.browser.assertView('all-nested-services', PO.tree.services());
        });
    });
});
