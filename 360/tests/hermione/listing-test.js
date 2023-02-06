const adminPage = require('../../../hermione/pages/admin');
const { gotoOrganization } = require('./helpers/common');

describe('Чаты', function () {
    it('diskforbusiness-255: Отображение чата платной организации', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-006', '5aR@h_006');
        await bro.url('/');

        await gotoOrganization(bro);

        await bro.yaWaitForVisible(
            adminPage.chatWidget(),
            'Не отобразился чат'
        );
    });

    it('diskforbusiness-253: Отображение чата бесплатной организации', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-sarah-test-005', '5aR@h_005');
        await bro.url('/');

        await gotoOrganization(bro);

        await bro.yaWaitForVisible(
            adminPage.chatWidget(),
            'Не отобразился чат',
            10000
        );
    });
});
