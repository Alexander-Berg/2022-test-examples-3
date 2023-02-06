const routes = require('../server/routers/routes').routes;
const hooks = require('../server/routers/hooks').appHooks;
const duffman = require('@yandex-int/duffman');

const app = duffman.express();

routes.forEach(({ name, path }) => {
    app.use(name, require(path));
});

hooks.forEach(({ hook }) => {
    hook(app);
});

module.exports = app;
