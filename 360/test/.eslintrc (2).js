module.exports = {
    extends: [
       '../.eslintrc.js'
    ],
    plugins: [
        'chai-friendly'
    ],
    'rules': {
        'max-len': [ 'warn', 200 ],
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
        setModelByMock: false,
        setModelsByMock: false,
        getModelMock: false,
        nsRequestMock: false,
        getModelMockByName: false,
        nsRequestMock: false,
        chai: false
    }
};
