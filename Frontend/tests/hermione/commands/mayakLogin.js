const url = require('url');
const config = require('config');

module.exports = function (login, passwd) {
    const aquaUrl = url.format({
        protocol: 'https',
        host: 'aqua.yandex-team.ru',
        pathname: 'auth.html',
        query: {
            mode: 'auth',
            login,
            passwd,
            host: `${config.get('passport.host')}/passport`,
            retpath: config.get('mayak.host'),
        },
    });

    return this.url(aquaUrl).waitForPageLoaded();
};
