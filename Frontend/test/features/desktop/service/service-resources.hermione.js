const PO = require('./PO');

describe('Страница сервиса', function() {
    describe('Положительные', function() {
        it('1. Сервис предоставляет ресурсы', async function() {
            // открыть сервис "Стафф" (/services/staff/)
            await this.browser.openIntranetPage({
                pathname: '/services/staff/',
            });
            // форма запроса ресурса откроется потом в новом табе
            const initialTabIds = await this.browser.getTabIds();
            // в правой части в описании сервиса есть блок "Ресурсы",
            // в котором указаны два ресурса - "Робот" и "API"
            await this.browser.waitForVisible(PO.service.resources(), 10000);
            await this.browser.assertView('service-resources', PO.service.resources());
            // кликнуть на ссылку "запросить"
            await this.browser.click(PO.service.resources.robot.requestLink());
            // произошёл переход на страницу с ресурсами сервиса "Стафф"
            // нужно понять, какой таб новый, и перейти на него
            const newTabIds = await this.browser.getTabIds();
            const tabsDiff = newTabIds.filter(id => !initialTabIds.includes(id));
            const targetTab = tabsDiff && tabsDiff[0];
            if (!targetTab) {
                throw new Error('Таб не открылся');
            }
            await this.browser.switchTab(targetTab);
            // открыта страница с ресурсами сервиса "Стафф"
            const url = await this.browser.yaGetParsedUrl();
            assert(url.pathname === '/services/staff/resources/' && url.search === '?new-resource=yes&new-resource-resource=92',
                'произошёл переход на страницу с ресурсами сервиса "Стафф"',
            );
            // на странице открыта форма запроса ресурса,
            // в форме заполнено только поле "Тип ресурса" значением "Стафф - Робот"
            await this.browser.waitForVisible(PO.resourceRequestForm(), 10000);
            await this.browser.assertView('resource-request-modal', PO.resourceRequestForm());
        });
        it('2. Сервис не предоставляет ресурсы', async function() {
            // открыть сервис "Автотестовый сервис" (/services/autotestservice2309/)
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            });
            // в правой части в описании сервиса есть блок "Ресурсы",
            // в котором указано "сервис не предоставляет ресурсы"
            await this.browser.waitForVisible(PO.service.resources(), 10000);
            await this.browser.assertView('service-no-resources', PO.service.resources());
        });
    });
});
