const _defaultsDeep = require('lodash/defaultsDeep');

const commonConfig = require('./config.common');

module.exports = _defaultsDeep(
  {
    /**
     * > Integration tests use a dynamic environment with a lot of dependencies,
     * > where any of them could be unstable from time to time.
     */
    retry: 2,
    plugins: {
      '@yandex-int/hermione-auth-commands': {
        env: 'prod'
      }
    }
  },
  commonConfig
);
