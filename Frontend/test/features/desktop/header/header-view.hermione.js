const PO = require('./PO');

async function assertHeaderView(user, lang) {
    return this.browser
        .openIntranetPage({
            pathname: '/error/403',
            query: { lang },
        }, {
            user,
        })
        .waitForVisible(PO.header(), 10000)
        .assertView(`header-${lang}-for-${user}`, PO.header(), { ignoreElements: PO.messenger() });
}

const users = [
    'robot-abc-002',
    'robot-abc-003',
    'robot-abc-004',
];

describe('Просмотр шапки', () => {
    describe('Внешний вид шапки', function() {
        for (const lang of ['ru', 'en']) {
            for (const user of users) {
                it(`(${lang}) Для юзера ${user}`, async function() {
                    return assertHeaderView.call(this, user, lang);
                });
            }
        }
    });
});
