/**
 * Configuring ESLint
 * @see {@link https://eslint.org/docs/user-guide/configuring}
 */
module.exports = {
    /**
     * @see {@link https://eslint.org/docs/user-guide/configuring#specifying-environments}
     */
    env: {
        commonjs: true,
        es6: true,
        node: true,
        jest: true,
    },

    /**
     * @see {@link https://eslint.org/docs/user-guide/configuring#extending-configuration-files}
     */
    extends: ['airbnb-base', 'eslint-config-prettier'],

    /**
     * @see {@link https://eslint.org/docs/user-guide/configuring#specifying-globals}
     */
    globals: {
        Atomics: 'readonly',
        SharedArrayBuffer: 'readonly',
    },

    /**
     * @see {@link https://eslint.org/docs/user-guide/configuring#specifying-parser-options}
     */
    parserOptions: {
        ecmaVersion: 2018,
    },

    /**
     * @see {@link https://eslint.org/docs/user-guide/configuring#configuring-rules}
     */
    rules: {
        indent: ['warn', 4],

        /**
         * TODO: remove follwoing rules after fixing files
         */
        'no-var': 'warn',
        'max-len': 'warn',
        'prefer-template': 'warn',
        'no-unused-vars': 'warn',
        'comma-dangle': 'warn',
        strict: 'warn',
        'no-console': 'warn',
        'no-use-before-define': 'warn',
        'key-spacing': 'warn',
        'one-var': 'warn',
        'prefer-destructuring': 'warn',
        'no-useless-escape': 'warn',
        'quote-props': 'warn',
        'no-multiple-empty-lines': 'warn',
        radix: 'warn',
        'no-restricted-globals': 'warn',
        camelcase: 'warn',
        'lines-around-directive': 'warn',
        'object-shorthand': 'warn',
        'no-param-reassign': 'warn',
        'import/newline-after-import': 'warn',
        eqeqeq: 'warn',
        'no-else-return': 'warn',
        'spaced-comment': 'warn',
        'prefer-const': 'warn',
        'no-prototype-builtins': 'warn',
        'dot-notation': 'warn',
        'consistent-return': 'warn',
    },
};
