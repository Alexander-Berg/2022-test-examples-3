module.exports = {
    rules: {
        'init-declarations': 0,
        'max-len': [2, 140],
        'max-nested-callbacks': 0,
        'max-statements': [2, 15],
        'no-unused-expressions': 0
    },
    extends: ['../server/.eslintrc.js'],
    globals: {
        require: true,
        describe: true,
        afterEach: true,
        beforeEach: true,
        before: true,
        it: true
    }
};
