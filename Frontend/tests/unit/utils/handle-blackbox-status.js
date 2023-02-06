const config = require('config');

const handleBlackboxStatus = require('../../../src/server/utils/handle-blackbox-status.js');

describe('utils/handleBlackboxStatus', () => {
    let sandbox;

    beforeEach(() => {
        sandbox = sinon.createSandbox();
        sandbox.stub(config, 'hostname').value('originurl.ru');
    });

    afterEach(() => sandbox.restore());

    it('должен возвращать указание не делать редирект, если от BB пришел статус VALID', () => {
        const status = 'VALID';
        assert.deepEqual(handleBlackboxStatus(status), { needRedirect: false });
    });

    it('должен возвращать указание сделать редирект на url подновления авторизационной куки, если от BB пришел статус NEED_RESET', () => {
        const status = 'NEED_RESET';
        const url = 'https://passport.yandex-team.ru/auth/update?retpath=https%3A%2F%2Foriginurl.ru';

        assert.deepEqual(handleBlackboxStatus(status), { needRedirect: true, url });
    });

    it('должен возвращать указание сделать редирект на страничку авторизации, если от BB не пришел статус VALID или NEED_RESET', () => {
        const status = 'INVALID';
        const url = 'https://passport.yandex-team.ru/auth?retpath=https%3A%2F%2Foriginurl.ru';

        assert.deepEqual(handleBlackboxStatus(status), { needRedirect: true, url });
    });
});
