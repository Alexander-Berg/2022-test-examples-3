const test = require('./jest.config.testament.js');

test.testMatch = [
    '**/platform.desktop/**/__spec__/**/*.business.testament.spec.js',
    '**/__spec__/**/*.business.desktop.testament.spec.js',
    '**/__spec__/**/*.business.testament.spec.js',
];

test.testPathIgnorePatterns = [];

module.exports = test;
