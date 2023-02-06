import path from 'path';

import { jestDefaultConfig, mergeConfigs } from './jest.config';
import { MBO_CORE_DIR } from '../../constants';

describe('jest.config', () => {
  it('mergeConfigs', () => {
    const ignorePatterns = ['<rootDir>/src/java', '<rootDir>/src/pages/documents'];

    const customConfig = {
      moduleNameMapper: {
        'react-syntax-highlighter/dist/esm/styles/.*': true,
        '\\.(css|less|scss|sass)$': 'custom-resolver',
      },
      coverageThreshold: {
        global: {
          branches: 99,
        },
      },
      modulePathIgnorePatterns: ignorePatterns,
      watchPathIgnorePatterns: ignorePatterns,
      testPathIgnorePatterns: ignorePatterns,
      coveragePathIgnorePatterns: ignorePatterns,
    };

    expect(mergeConfigs(jestDefaultConfig, customConfig)).toEqual({
      testEnvironment: 'jsdom',
      testMatch: ['<rootDir>/src/**/?(*.)(spec|test).ts?(x)'],
      setupFilesAfterEnv: [path.resolve(MBO_CORE_DIR, 'static/extend-expect.ts')],
      moduleNameMapper: {
        '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$': path.resolve(
          MBO_CORE_DIR,
          'static/fileMock.js'
        ),
        '\\.(css|less|scss|sass)$': 'custom-resolver',
        '^src/(.*)': '<rootDir>/src/$1',
        '^test/(.*)': '<rootDir>/test/$1',
        'react-syntax-highlighter/dist/esm/styles/.*': require.resolve('identity-obj-proxy'),
      },
      transformIgnorePatterns: ['/node_modules/(?!ramda|@yandex-market/mbo-|react-select.*?).+\\.js$'],
      collectCoverageFrom: jestDefaultConfig.collectCoverageFrom,
      coverageThreshold: {
        global: {
          branches: 99,
          functions: 35,
          lines: 43,
          statements: 43,
        },
      },
      transform: jestDefaultConfig.transform,
      modulePathIgnorePatterns: ['/node_modules/'].concat(ignorePatterns),
      watchPathIgnorePatterns: ['/node_modules/'].concat(ignorePatterns),
      testPathIgnorePatterns: ['/node_modules/'].concat(ignorePatterns),
      coveragePathIgnorePatterns: ['/node_modules/'].concat(ignorePatterns),
    });
  });
});
