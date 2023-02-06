require('v8-compile-cache');

const transform = {};

if (process.env.TESTAMENT_RENDER_PROCESS) {
    process.env.APP_ENV = 'server';
    transform[/.+\/__pageObject\/.+/.source] = '@yandex-market/testament/transformers/null';
    transform[/.+\/(widgets|@yandex-market\/mandrel\/devTools)\/.*\/epics.js$/.source] = ['@yandex-market/testament/transformers/emptyEpics'];
} else {
    process.env.APP_ENV = 'browser';
    transform[/.+\/(platform\..+?|src)\/resolvers\/.+/.source] = '@yandex-market/testament/transformers/remoteResolver';
    transform[/.+\/(widgets|@yandex-market\/mandrel\/devTools)\/.*\/(controller.js|legacyController.js|controller\/index.*.js)$/.source] = '@yandex-market/testament/transformers/null';
    // transform[/.+\/bcm\/.+/.source] = '@yandex-market/testament/transformers/null';
}

const test = {
    displayName: 'test',
    testURL: 'http://localhost',
    roots: [
        '<rootDir>/widgets',
        '<rootDir>../src/widgets',
        '<rootDir>../../src/widgets',
    ],
    collectCoverage: false,
    setupFiles: ['<rootDir>/configs/jest/setup.js'],
    testMatch: [
        '**/platform.touch/**/__spec__/**/*.testament.spec.js',
        '**/__spec__/**/*touch.testament.spec.js',
    ],
    transform: {
        ...transform,
        '^.+\\.(js|jsx|mjs)?$': '<rootDir>/configs/jest/preprocessor.js',
        '.+\\.(styl|css)$': '<rootDir>/../configs/jest/identityTransform.js',
        '.+\\.(png|jpe?g|svg)$':
            '<rootDir>/../configs/jest/identityTransform.js',
    },
    transformIgnorePatterns: [
        // eslint-disable-next-line max-len
        '<rootDir>/node_modules/(?!(@yandex-market/levitan-gui|@yandex-levitan/market|entities|@yandex-market/levitan-annex|@yandex-market/cia|@yandex-market/beton)/)',
        '<rootDir>/../node_modules/(?!(@yandex-market/levitan-gui|@yandex-levitan/market|entities|@yandex-market/levitan-annex|@yandex-market/cia|@yandex-market/beton)/)',
        '<rootDir>/../../node_modules/(?!(@yandex-market/levitan-gui|@yandex-levitan/market|entities|@yandex-market/levitan-annex|@yandex-market/cia|@yandex-market/beton)/)',
    ],
    // index файл не обязательно должен быть, могут быть только платформенные зависимости
    moduleFileExtensions: ['js', 'json', 'touch.styl', 'styl'],
    moduleNameMapper: {
        '^.+\\.(css|styl)': '<rootDir>/../configs/jest/identityObjProxy.js',
        '^.+\\.svg$': require.resolve('identity-obj-proxy'),
        '^components/(.*)': '<rootDir>/components/$1',
        '^constants/(.*)': '<rootDir>/app/constants/$1',
    },
    setupFilesAfterEnv: ['@testing-library/jest-dom', '<rootDir>/configs/jest/after-env'],
    moduleDirectories: [
        'node_modules',
        // нужно чтобы импорты в левитане работали в тестах:
        '<rootDir>/node_modules/(?!(@yandex-market/levitan-gui|@yandex-levitan/market|@yandex-market/levitan-annex))',
        '<rootDir>/../node_modules/(?!(@yandex-market/levitan-gui|@yandex-levitan/market|@yandex-market/levitan-annex))',
        '<rootDir>/../../node_modules/(?!(@yandex-market/levitan-gui|@yandex-levitan/market|@yandex-market/levitan-annex))',
    ],

    testRunner: '@yandex-market/testament/runner',
    testEnvironment: '@yandex-market/testament/env',
    testEnvironmentOptions: {
        configPath: __filename,
        argv: process.argv,
    },
    testTimeout: 600000,
    resolver: '@yandex-market/testament/resolver',
    cache: true,
    haste: {
        defaultPlatform: 'touch',
    },
    maxWorkers: '90%',
};

module.exports = test;
