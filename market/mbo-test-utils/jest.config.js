const {
  withDefaultConfig,
  withGlobalsTsConfig,
  withCollectCoverageFrom,
} = require('@yandex-market/mbo-dev-utils/jest');
const R = require('ramda');

const withConfig = R.compose(
  withCollectCoverageFrom(['src/**/*.{ts,tsx}']),
  withGlobalsTsConfig('<rootDir>/tsconfig.json'),
  withDefaultConfig
);

const config = withConfig({});

module.exports = config;
