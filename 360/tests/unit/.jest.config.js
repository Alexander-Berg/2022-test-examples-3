const path = require('path');

module.exports = {
    displayName: 'www',
    transformIgnorePatterns: [
        'node_modules/(?!(lego-on-react|ufo-helpers|ufo-rocks2|@ps-int/ufo-rocks|@ps-int/ufo-helpers|@ps-int/mg-theme|@ps-int/ps-components)/)'
    ],
    globals: {
        __DEV__: true,
        IS_TOUCH: false,
        RUNTIME: 'browser',
        Ya: {
            Rum: {}
        }
    },
    collectCoverageFrom: [
        'components/rocks/**/*.js',
        'components/redux/**/*.js',
    ],
    testRegex: [
        '.*tests/unit.*(test|stories).js$',
        '.*components/redux.*test.js$'
    ],
    coverageReporters: ['json'],
    rootDir: path.resolve(__dirname, '../../'), // прокидываем корень jest в корень проекта
    // далее rootDir ссылается на корень проекта
    coverageDirectory: '<rootDir>/tests/unit/__coverage__',
    moduleFileExtensions: ['ts', 'tsx', 'json', 'js', 'client.js', 'default.js'],
    modulePaths: [
        '<rootDir>/components',
        '<rootDir>/node_modules',
    ], // разрешаем магические пути в импортах
    setupFilesAfterEnv: ['<rootDir>/tests/unit/setup-tests.js'],
    moduleNameMapper: {
        '\\.(css|styl)$': '<rootDir>/node_modules/jest-css-modules',
        '\\.(jpg|jpeg|png|gif|svg)$': '<rootDir>/tests/unit/mocks/dummy.js'
    },
    snapshotSerializers: ['enzyme-to-json/serializer'],
    reporters: [
        'default',
        ['jest-html-reporters', {
            publicPath: './tmp/jest-html-report',
            filename: 'index.html'
        }]
    ]
};
