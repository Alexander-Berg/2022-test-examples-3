const stoker = require('./stoker');

const RELEASE_HOST = 'WEB__RENDERER_GOODWIN_RC';
const isRelease = () => Boolean(process.env.RELEASE_SERVICE);

let goodwinHost;

module.exports.getHost = async() => {
    if (goodwinHost !== undefined) {
        return goodwinHost;
    }

    if (process.env.GOODWIN_HOST) { // Тесты на локальной бете
        goodwinHost = process.env.GOODWIN_HOST;
    } else if (isRelease()) { // Тесты в релизе
        goodwinHost = RELEASE_HOST;
    } else if (process.env.GOODWIN_GET_HOST) { // Тесты в PR
        goodwinHost = await stoker.getHost();
    } else { // Тесты на продакшене
        goodwinHost = null;
    }

    return goodwinHost;
};
