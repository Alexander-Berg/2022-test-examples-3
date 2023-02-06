module.exports = {
    rules: {
        'init-declarations': 0,
        'max-len': [2, 140],
        'max-nested-callbacks': 0,
        'no-unused-expressions': 0
    },
    env: {
        node: true,
        mocha: true
    },
    extends: ['../server/.eslintrc.js']
};
