module.exports = {
  rootDir: '../../../',
  resetMocks: true,
  testMatch: ['<rootDir>/client/**/*.spec.js'],
  setupFilesAfterEnv: ['<rootDir>/client/tests/unit/jest.setup.js'],
  moduleNameMapper: {
    '^@/(.+)$': '<rootDir>/client/$1',
    '^Constants/(.+)$': '<rootDir>/constants/$1'
  },
  collectCoverageFrom: ['<rootDir>/client/**/*.js'],
  coverageDirectory: '<rootDir>/server/client/unit/coverage',
  coverageReporters: ['text', 'lcov']
};
