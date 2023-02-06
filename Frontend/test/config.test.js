const config = require('../configs/current/config');
const getYENV = require('../tools/get-yenv');

const { YENV = getYENV() } = process.env;

const staticPaths = {
    testing: {
        http: 'https://1.yastat.net/s3/turbo/',
    },
    production: {
        http: 'https://yastatic.net/s3/turbo-static/_/',
    },
    development: {
        http: '/static/turbo/',
    },
}[YENV];

// Этот тест работает только если всё собрано в окружении тестинга или прода.
// Падение в деве можно игнорировать.
describe('config', () => {
    it('должен вернуть дефолтный хост для статики по умолчанию', () => {
        const baseConfig = config();
        expect(baseConfig.turboStaticPath).toEqual(staticPaths.http);
    });

    it('должен вернуть дефолтный хост для яндексовой статики по умолчанию', () => {
        const baseConfig = config();
        expect(baseConfig.commonStaticPath).toEqual('https://yastatic.net');
    });
});
