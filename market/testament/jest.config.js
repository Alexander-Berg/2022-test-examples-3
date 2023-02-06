const transform = {};

if (process.env.TESTAMENT_RENDER_PROCESS) {
    transform[/.+\/__pageObject\/.+/.source] = '<rootDir>/dist/transformers/null.js';
    transform[/.+\/epics.js$/.source] = ['<rootDir>/dist/transformers/emptyEpics.js'];
} else {
    transform[/.+\/controller\.[jt]s$/.source] = '<rootDir>/dist/transformers/null.js';
    transform[/.+\/resolvers\/.+/.source] = ['<rootDir>/dist/transformers/remoteResolver.js', {foo: 'bar'}];
    // transform[/.+\/bcm\/.+/.source] = '<rootDir>/../../configs/jest/nullTransform.js';
}

module.exports = {
    roots: ['<rootDir>/src'],
    transform: {
        ...transform,
        '^.+\\.tsx?$': '@swc/jest',
    },
    testMatch: ['**/*.(spec|test).[jt]s?(x)'],
    moduleFileExtensions: ['js', 'ts', 'tsx', 'json'],
    transformIgnorePatterns: ['/node_modules/uuid/'],
    setupFiles: ['<rootDir>/dist/setup'],
    setupFilesAfterEnv: ['<rootDir>/dist/setup-env'],

    testEnvironment: '<rootDir>/dist/env',
    testEnvironmentOptions: {
        configPath: __filename,
        argv: process.argv,
    },
    testTimeout: 300000,
    snapshotSerializers: ['<rootDir>/dist/snapshotSerializer/html'],

    testRunner: 'jest-circus/runner',
    moduleLoader: '<rootDir>/dist/runtime',
    resolver: '<rootDir>/dist/resolver',

    reporters: [
        'default',
        ['<rootDir>/dist/reporter/allure', {
            saveTo: 'html_reports',
        }],
        ['<rootDir>/dist/reporter/metrics', {
            saveTo: 'json_reports/metrics.json',
        }],
    ],
};
