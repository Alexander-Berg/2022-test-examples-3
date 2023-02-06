module.exports = {
    root: true,
    rules: {
        quotes: [2, 'single'],
        indent: [2, 4],
        'key-spacing': [2],
        'quote-props': [2, 'as-needed'],
        'comma-spacing': [2],
        'object-curly-spacing': [2, 'always'],
    },
    env: {
        node: true,
        es6: true,
    },
    parserOptions: {
        ecmaVersion: 2018,
    }
};
