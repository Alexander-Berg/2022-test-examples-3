const commonConfig = require('./../common/config');

// В некоторых случаях требуется явно указать путь от которого получать статику.
// Мы делаем это в отдельных конфигах (дева и тестинга), чтобы не тащить в продакшн.
const { FREEZE_PATH = '' } = process.env;

module.exports = function(props) {
    const baseConfig = commonConfig(props);

    const { turboStaticPath } = baseConfig;

    return Object.assign(
        baseConfig,
        {
            env: 'testing',
            pageBundle: 'pages/all',
            turboStaticPath: FREEZE_PATH + turboStaticPath,
            backendPath: 'https://l7test.yandex.ru/turbofeedback/udata',
            lcUserEndpoint: 'https://dc-develop.common.yandex.ru/services/user',
        }
    );
};
