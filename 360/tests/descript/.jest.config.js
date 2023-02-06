const path = require('path');

module.exports = {
    displayName: 'www-descript',
    transformIgnorePatterns: ['node_modules'],
    testRegex: '.*tests/descript/.*test\.js$',
    rootDir: path.resolve(__dirname, '../../'),
    coverageDirectory: '<rootDir>/tests/descript/__coverage__'
};
