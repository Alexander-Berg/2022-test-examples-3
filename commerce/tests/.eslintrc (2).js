module.exports = {
    env: {
        node: true,
        mocha: true
    },
    extends: ['../.eslintrc.js'],
    rules: {
        'global-require': 0,
        'no-unused-expressions': 0,
        'max-statements': 0,
        'max-nested-callbacks': 0
    }
};
