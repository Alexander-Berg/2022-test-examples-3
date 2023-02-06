'use strict';

module.exports = {
  rootDir: '../../../',
  resetMocks: true,
  testEnvironment: 'node',
  testMatch: ['<rootDir>/server/**/*.spec.js'],
  setupFilesAfterEnv: ['<rootDir>/server/tests/unit/jest.setup.js'],
  moduleNameMapper: {
    '^@/(.+)$': '<rootDir>/server/$1',
    '^Constants/(.+)$': '<rootDir>/constants/$1'
  },
  collectCoverageFrom: ['<rootDir>/server/**/*.js'],
  coverageDirectory: '<rootDir>/server/tests/unit/coverage',
  coverageReporters: ['text', 'lcov']
};
