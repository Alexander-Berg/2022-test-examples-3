'use strict';

module.exports = {
    rootDir: '../..',
    resetMocks: true,
    testEnvironment: 'node',
    testMatch: ['<rootDir>/**/*.spec.js'],
    setupFilesAfterEnv: ['<rootDir>/tests/unit/jest.setup.js'],
    cacheDirectory: '<rootDir>/tests/unit/.cache',
    // Coverage
    collectCoverageFrom: [
        '<rootDir>/models/**/*.js',
        '<rootDir>/services/**/*.js',
        '<rootDir>/filters/**/*.js',
        '<rootDir>/utils/**/*.js',
        '!**/node_modules/**'
    ],
    coverageDirectory: '<rootDir>/tests/unit/coverage',
    coverageReporters: ['lcov', 'text-summary'],
    coverageThreshold: {
        global: {
            statements: 40,
            branches: 32,
            functions: 39,
            lines: 41
        }
    },
    // Aliases
    moduleNameMapper: {
        '^@/(.+)$': '<rootDir>/$1'
    }
};
