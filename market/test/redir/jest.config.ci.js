const jestCommonConfig = require('./jest.config.common');

/**
 * @see {@link https://jestjs.io/docs/en/configuration}
 */
module.exports = {
    ...jestCommonConfig,
    collectCoverage: true,
    displayName: 'ci',
    notify: false,
    bail: false,
    verbose: true,
};
