import 'hermione';
import chai from 'chai';

import NoAuth from '../../page-objects/noAuth';
import {logout} from '../../helpers';

// id: 'ocrm-58'
// issue: 'OCRM-2837',
describe('ocrm-58: Блок "Ошибка доступа"', async function() {
    beforeEach(async function() {
        await logout(this);

        return this.browser.url('/');
    });

    it('должен отображаться на странице и содержать ссылку для авторизации', async function() {
        const entryPoint = new NoAuth(this.browser);

        const isVisible = await entryPoint.isDisplayed();

        await chai.expect(isVisible).to.equal(true, 'Блок виден');

        const link = await entryPoint.getLink();
        const expectedLink = 'https://passport.yandex-team.ru/auth/?retpath=https://ow.tst.market.yandex-team.ru';

        return chai.expect(link).to.equal(expectedLink, 'Ссылка для авторизации корректная');
    });
});
