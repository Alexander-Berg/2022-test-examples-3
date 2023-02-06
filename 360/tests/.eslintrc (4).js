module.exports = {
    parserOptions: {
        ecmaVersion: 2018
    },
    globals: {
        jest: false,
        popFnCalls: false,
        describe: false,
        it: false,
        beforeEach: false,
        afterEach: false,
        beforeAll: false,
        afterAll: false,
        expect: false,
        config: false,
        hermione: false
    },
    extends: '../.eslintrc.js'
};
