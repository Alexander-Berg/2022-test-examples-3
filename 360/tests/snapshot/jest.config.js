module.exports = {
  rootDir: '../../../',
  resetMocks: true,
  testMatch: ['<rootDir>/client/**/*.snapshot.js'],
  setupFilesAfterEnv: ['<rootDir>/client/tests/snapshot/jest.setup.js'],
  snapshotSerializers: ['enzyme-to-json/serializer'],
  moduleNameMapper: {
    '\\.(svg|png)$': '<rootDir>/client/tests/snapshot/mocks/file.js',
    '\\.css$': 'identity-obj-proxy',
    '^@/(.+)$': '<rootDir>/client/$1',
    '^Constants/(.+)$': '<rootDir>/constants/$1'
  },
  collectCoverageFrom: ['<rootDir>/client/components/**/*.js'],
  coverageDirectory: '<rootDir>/client/tests/snapshot/coverage',
  coverageReporters: ['text', 'lcov']
};
