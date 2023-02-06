const {prepareRoutes} = require('@yandex-market/market-shared/src/app/routes/utils');
const jsonApiRoutes = require('@yandex-market/market-shared/src/app/routes/jsonApi');
const getExternalRoutes = require('@yandex-market/market-shared/src/app/routes/external');
const nodeConfig = require('./node');
const platformRoutes = require('@root/configs/route/touch/routes');

const externalRoutes = getExternalRoutes(nodeConfig);

module.exports = prepareRoutes({
    routes: platformRoutes,
    externalRoutes,
    jsonApiRoutes,
})
