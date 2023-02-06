const baseBdd = require('mocha/lib/interfaces/bdd');

module.exports = function (suite) {
    baseBdd(suite);

    suite.on('pre-require', function (context) {
        context.specs = (title, fn) => context.describe(formatTitle(title), fn);
    });
};

function formatTitle(obj) {
    return typeof obj === 'string' ? obj : [
        'category',
        'feature',
        'type',
        'experiment',
    ]
        .map((prop) => obj[prop])
        .filter(Boolean)
        .join(' / ');
}
