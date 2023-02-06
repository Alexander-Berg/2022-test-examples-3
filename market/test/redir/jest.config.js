const jestCommonConfig = require('./jest.config.common');

/**
 * @see {@link https://jestjs.io/docs/en/configuration}
 */
module.exports = {
    ...jestCommonConfig,
    collectCoverage: false,
    displayName: 'local',
    notify: true,
    bail: true,
    verbose: false,
};
