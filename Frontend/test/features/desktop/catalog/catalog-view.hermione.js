const PO = require('./PO');

function assertCatalogueView(user, lang) {
    return this.browser
        .openIntranetPage({
            pathname: '/',
            query: {
                lang,
                // фильтр по статусам, чтобы список корневых сервисов влез в один экран
                states: ['develop', 'needinfo'],
            },
        }, {
            user,
        })
        .waitForVisible(PO.catalogue.treeRow(), 10000)
        .assertView(`catalogue-${lang}-for-${user}`, PO.catalogue());
}

const users = ['robot-abc-002', 'robot-abc-003', 'robot-abc-004'];

describe('Просмотр каталога', () => {
    describe('Положительные', function() {
        // цикл снаружи - понятно на какой роли падает, но больше тестов
        // (не один на все роли, а по тесту на каждую)
        for (const user of users) {
            it(`(ru) Просмотр каталога для юзера ${user}`, function() {
                return assertCatalogueView.call(this, user, 'ru');
            });
        }

        for (const user of users) {
            it(`(en) Просмотр каталога для юзера ${user}`, function() {
                return assertCatalogueView.call(this, user, 'en');
            });
        }
    });
});
