const routes = require('./../../../routes').default;

module.exports = {
    PRODUCTS: [
        (req, res, next) => {
            req.path = '/products';
            next();
        },
        routes.products,
    ],
    AVIA_SEARCH_CHECK: routes.aviaSearchCheck,
    AVIA_SEARCH_CHECK_V2: [
        (req, res, next) => {
            try {
                req.params = req.params || {};
                req.params.version = 'v2.0';
            } catch (exp) {
                console.log(exp);
            }
            next();
        },
        routes.aviaSearchCheck,
    ],
    AVIA_SEARCH_START: routes.aviaSearchStart,
    CLIENT_EVENT: routes.clientEvent,
    SOVETNIK: routes.saveSovetnikInfo,
    SETTINGS: routes.settings,
    DISABLED: routes.sovetnikDisabled,
};
