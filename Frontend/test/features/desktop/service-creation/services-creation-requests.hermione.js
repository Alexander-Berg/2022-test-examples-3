const PO = require('./PO');
const langs = ['ru', 'en'];

describe('Запросы создания сервисов', () => {
    describe('Положительные', () => {
        langs.forEach(lang => {
            it(`Фильтры(${lang})`, async function() {
                // открыть запросы создания сервисов
                // (/approves/services-creation/?exp_flags=service-creation&lang=${lang})
                await this.browser.openIntranetPage({
                    pathname: '/approves/services-creation/',
                    query: { exp_flags: 'service-creation', lang },
                });

                await this.browser.waitForVisible(PO.requests(), 10000);
                await this.browser.waitForVisible(PO.requests.spin(), 10000, true);
                // открылись запросы с фильтром Непосредственные [direct-requests]
                await this.browser.assertView('direct-requests', PO.requests());

                // нажать на фильтр С учетом иерархии
                await this.browser.click(PO.requests.hierarchyFilterButton());
                await this.browser.waitForVisible(PO.requests.checkedHierarchyFilterButton(), 10000);
                await this.browser.waitForVisible(PO.requests.spin(), 10000, true);
                // открылись запросы с фильтром С учетом иерархии [hierarchy-requests]
                await this.browser.assertView('hierarchy-requests', PO.requests());

                // нажать на фильтр Непосредственные
                await this.browser.click(PO.requests.directFilterButton());
                await this.browser.waitForVisible(PO.requests.checkedDirectFilterButton(), 10000);
                await this.browser.waitForVisible(PO.requests.spin(), 10000, true);
                // открылись запросы с фильтром Непосредственные [direct-requests-again]
                await this.browser.assertView('direct-requests-again', PO.requests());
            });
        });

        it('Подтверждение запроса', async function() {
            // открыть запросы создания сервисов
            // (/approves/services-creation/?exp_flags=service-creation)
            await this.browser.openIntranetPage({
                pathname: '/approves/services-creation/',
                query: { exp_flags: 'service-creation' },
            });

            await this.browser.waitForVisible(PO.requests.firstRequest(), 10000);
            // первый запрос перед подтверждением [request-before-approvement]
            await this.browser.assertView('request-before-approvement', PO.requests.firstRequest());

            // нажать кнопку подтверждения в первом запросе
            await this.browser.click(PO.requests.firstRequest.approveLink());

            await this.browser.waitForVisible(PO.requests.firstRequest.approvedLink(), 10000);
            // запросы, первый запрос подтвержден [approve-request]
            await this.browser.assertView('approve-request', PO.requests.firstRequest());
        });

        it('Отклонение запроса', async function() {
            // открыть запросы создания сервисов
            // (/approves/services-creation/?exp_flags=service-creation)
            await this.browser.openIntranetPage({
                pathname: '/approves/services-creation/',
                query: { exp_flags: 'service-creation' },
            });

            await this.browser.waitForVisible(PO.requests.firstRequest(), 10000);
            // первый запрос перед отказом [request-before-rejection]
            await this.browser.assertView('request-before-rejection', PO.requests.firstRequest());

            // нажать кнопку отказа в первом запросе
            await this.browser.click(PO.requests.firstRequest.rejectLink());

            await this.browser.waitForVisible(PO.requests.firstRequest.rejectedLink(), 10000);
            // запросы, первый запрос отказан [reject-request]
            await this.browser.assertView('reject-request', PO.requests.firstRequest());
        });
    });
});
