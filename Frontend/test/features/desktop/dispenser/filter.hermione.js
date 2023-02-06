const PO = require('./PO');

const suggestText = 'Сервис для автотестов';
const suggestSelector = `[title="${suggestText}"]`;

describe('Dispenser: Список заявок - Фильтры', function() {
    describe('Панель фильтров', function() {
        it('1. Фильтр по сервису', async function() {
            await this.browser
                .openIntranetPage({
                    pathname: '/hardware',
                }, { user: 'robot-abc-002' });

            // код перенесен из тестов фильтра каталога
            // (TODO: DISPENSER-2552 подумать о абстракции этой части сценария)
            // исходное состояние списка
            await this.browser
                .waitForVisible(PO.list(), 5000)
                .waitForVisible(PO.list.item1(), 5000)
                .assertView('initial', PO.wrapper());

            let parsedUrl = await this.browser.yaGetParsedUrl();

            const unsetUrlParams = [
                'summary',
                'author',
                'responsible',
                'showNested',
                'provider',
                'reason',
                'campaignOrder',
                'importantFilter',
                'unbalancedFilter',
                'owningCostGreaterOrEquals',
                'owningCostLessOrEquals',
            ];

            // в URL отсуствуют параметры фильтрации
            assert(
                unsetUrlParams.concat([
                    'page',
                    'service',
                    'status',
                ]).every(param => !parsedUrl.searchParams.has(param)),
                'В урле присутствуют параметры фильтрации',
            );

            await this.browser
                .waitForVisible(PO.filters.service(), 5000)
                .customSetValue(PO.filters.service.input(), 'автотест')
                .waitForVisible(suggestSelector, 5000)
                .click(suggestSelector);

            await this.browser
                // Способ дождаться смены контента списка заявок
                // На появление спиннера опираться нельзя, потому что при чтении из дампов он иногда не появляется
                // из-за быстрого ответа, тест плавает
                // До фильтрации в списке есть сервис, который пропадает после фильтрации. Ждём, пока он исчезнет.
                // При переснятии дампов набор заявок до фильтрации может измениться, тогда селектор нужно поменять
                .waitForVisible(PO.list.preFilterService(), 5000, true)
                .waitForVisible(PO.list(), 5000)
                .assertView('filtered', PO.wrapper());

            await this.browser
                .yaAssertUrlParam('service', '3407');

            parsedUrl = await this.browser.yaGetParsedUrl();

            // в URL отсуствуют параметры кроме приведенных выше
            assert(
                unsetUrlParams.every(param => !parsedUrl.searchParams.has(param)),
                'В урле присутствуют лишние параметры фильтрации',
            );

            await this.browser
                .click(PO.filters.reset())
                .waitForVisible(PO.filters.service.chosen(), 2000, true);

            return true;
        });

        it('2. Вшитый фильтр страницы сервиса', function() {
            return this.browser
                .openIntranetPage({
                    pathname: '/services/serviceforrobot003/hardware',
                }, { user: 'robot-abc-002' })
                .waitForVisible(PO.list.item1())
                .assertView('filtered-by-service', PO.wrapper());
        });
    });

    describe('Постраничная навигация', function() {
        it('1. Фильтр по номеру страницы', function() {
            return this.browser
                .openIntranetPage({
                    pathname: '/hardware',
                    query: {
                        page: '1',
                    },
                }, { user: 'robot-abc-002' })
                .waitForVisible(PO.list(), 5000)
                .waitForVisible(PO.pagination.p2(), 5000)
                .click(PO.pagination.p2())
                .yaAssertUrlParam('page', '2');
        });
    });
});
