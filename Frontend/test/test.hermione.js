const getCurrentEnv = require('../../../helpers/getCurrentEnv');

describe('Главная / Главная страница [B2B]', function() {
    it('Открыть главную', function() {
        const env = getCurrentEnv(this);

        return this.browser
            .yaLogin(env)
            .openPage('', '/search/directory', env)
            .waitForVisible('.content')
            .assertView('plain', 'html');
    });
});
