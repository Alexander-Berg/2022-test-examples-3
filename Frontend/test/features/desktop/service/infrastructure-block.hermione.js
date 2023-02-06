const PO = require('./PO');

describe('Страница сервиса', function() {
    describe('Положительные', function() {
        it('1. Отображение заполненного блока "Инфраструктура"', async function() {
            // открыть страницу сервиса "Каталог сервисов Яндекса (ABC)"
            await this.browser.openIntranetPage({
                pathname: '/services/abc/',
            }, { user: 'robot-abc-001' });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeamScopes(), 10000);
            // ждём, что загрузились блоки сверху, чтобы скриншот не смазывался
            await this.browser.waitForVisible(PO.serviceDescription.content(), 7000);
            await this.browser.waitForVisible(PO.serviceActivity.content(), 7000);
            await this.browser.waitForVisible(PO.infrastructureBlock.content(), 5000);
            // в правой части есть блок "Инфраструктура", в блоке указаны ссылки
            await this.browser.waitForVisible(PO.infrastructureBlock(), 1000);
            await this.browser.assertView(
                'infrastructure-block',
                PO.infrastructureBlock(),
                {
                    ignoreElements: [PO.infrastructureBlock.icons()],
                },
            );
        });
        it('2. Проверка ссылок в блоке "Инфраструктура"', async function() {
            // открыть страницу сервиса "Каталог сервисов Яндекса (ABC)"
            await this.browser.openIntranetPage({
                pathname: '/services/abc/',
            }, { user: 'robot-abc-001' });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeamScopes(), 10000);
            await this.browser.waitForVisible(PO.infrastructureBlock.content(), 5000);
            // проверяем ссылку на Warden
            let wardenUrl = await this.browser.getAttribute(
                PO.infrastructureBlock.warden.url(), 'href');
            assert.strictEqual(wardenUrl, 'https://warden.z.yandex-team.ru/components/abcd/s/abc/');
            // проверяем ссылку на "Чат SPI"
            let spiUrl = await this.browser.getAttribute(
                PO.infrastructureBlock.spiChat.url(), 'href');
            assert.strictEqual(spiUrl, 'https://t.me/joinchat/sJJKUjAFzopiMDcy');
            // проверяем ссылку на "Пресет в infra"
            let infraUrl = await this.browser.getAttribute(
                PO.infrastructureBlock.infra.url(), 'href');
            assert.strictEqual(infraUrl, 'https://infra.yandex-team.ru/timeline?preset=Zft6tfk3eXT');
            // проверяем ссылку на "Аркадия frontend/services/abc"
            let arcadiaFrontUrl = await this.browser.getAttribute(
                PO.infrastructureBlock.arcadia.frontUrl(), 'href');
            assert.strictEqual(arcadiaFrontUrl,
                'https://arcanum-test.yandex.net/arc/trunk/arcadia/frontend/services/abc/');
            // проверяем ссылку на "Аркадия intranet/plan"
            let arcadiaBackUrl = await this.browser.getAttribute(
                PO.infrastructureBlock.arcadia.backUrl(), 'href');
            assert.strictEqual(arcadiaBackUrl,
                'https://arcanum-test.yandex.net/arc/trunk/arcadia/intranet/plan/');
        });
        it('3. Отображение пустого блока "Инфраструктура"', async function() {
            // открыть страницу сервиса "autotest-contacts"
            await this.browser.openIntranetPage({
                pathname: '/services/autotest-contacts/',
            }, { user: 'robot-abc-001' });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeamScopes(), 10000);
            await this.browser.waitForVisible(PO.infrastructureBlock.content(), 5000);
            // в правой части есть блок "Инфраструктура", в блоке написано "пусто"
            await this.browser.waitForVisible(PO.infrastructureBlock.emptyBlock(), 500);
            await this.browser.assertView('infrastructure-block-empty', PO.infrastructureBlock());
        });
    });
});
