const baseBdd = require('mocha/lib/interfaces/bdd');
const SURFACES = require('../lib/uniproxy/surfaces');

const setSurface = (suite, surface) => {
    if (suite.tests) {
        suite.tests.forEach(test => test.ctx.surface = surface);
    }

    if (suite.suites) {
        suite.suites.forEach(item => setSurface(item, surface));
    }
};

const formatTitle = obj => {
    if (typeof obj === 'string') return obj;

    return ['feature', 'type', 'experiment']
        .map(prop => obj[prop])
        .filter(Boolean)
        .join(' / ');
};

module.exports = function(suite) {
    baseBdd(suite);

    suite.on('pre-require', function(context) {
        context.SURFACES = SURFACES;
        context.specs = function(title, surface, fn) {
            if (typeof surface === 'function') {
                fn = surface;
                surface = undefined;
            }

            const describe = context.describe(formatTitle(title), fn);

            setSurface(describe, surface);

            return describe;
        };
    });
};
