'use strict';

module.exports = {
  rootDir: '../../../',
  resetMocks: true,
  testEnvironment: 'node',
  testMatch: ['<rootDir>/server/routes/**/*.integration.js'],
  setupFilesAfterEnv: ['<rootDir>/server/tests/integration/jest.setup.js'],
  moduleNameMapper: {
    '^@/(.+)$': '<rootDir>/server/$1',
    '^Constants/(.+)$': '<rootDir>/constants/$1'
  },
  collectCoverageFrom: ['<rootDir>/server/routes/**/*.js'],
  coverageDirectory: '<rootDir>/server/tests/integration/coverage',
  coverageReporters: ['text', 'lcov']
};
