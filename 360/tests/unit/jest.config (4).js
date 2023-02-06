'use strict';

module.exports = {
    rootDir: '../..',
    resetMocks: true,
    testMatch: ['<rootDir>/**/*.spec.*'],

    setupFilesAfterEnv: ['<rootDir>/tests/unit/jest.setup.js'],
    moduleFileExtensions: ['ts', 'tsx', 'js'],
    cacheDirectory: '<rootDir>/tests/unit/.cache',
    // Coverage
    collectCoverageFrom: [
        '<rootDir>/utils/**/*.ts',
        '<rootDir>/features/**/*.ts',
        '!<rootDir>/features/**/actions.ts',
        '!<rootDir>/features/**/constants.ts',
        '!<rootDir>/features/**/components/**',
        '!<rootDir>/features/**/hocs/**'
    ],
    coverageDirectory: '<rootDir>/tests/unit/coverage',
    coverageReporters: ['lcov', 'text-summary'],
    coverageThreshold: {
        global: {
            statements: 62,
            branches: 42,
            functions: 53,
            lines: 62
        }
    },
    // Aliases
    moduleNameMapper: {
        '^@/features/fraud/sagas/maybeStartChallengeSaga':
            '<rootDir>/tests/unit/mocks/index.ts',
        '^@/\\.\\./types/common': '<rootDir>/../types/common.ts',
        '^@/(.+)$': '<rootDir>/$1'
    }
};
