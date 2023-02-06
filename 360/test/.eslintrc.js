module.exports = {
    plugins: [
        'chai-friendly'
    ],
    'rules': {
        'no-unused-expressions': 0,
        'chai-friendly/no-unused-expressions': 2
    },
    "parserOptions": {
        "ecmaVersion": 6,
    },
    globals: {
        sinon: false,
        describe: false,
        beforeEach: false,
        afterEach: false,
        it: false,
        expect: false,
        xdescribe: false,
        xit: false,
        mock: false,
        before: false,
        after: false,
        chai: false
    }
};
