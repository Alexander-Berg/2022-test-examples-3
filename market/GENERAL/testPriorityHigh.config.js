/**
 * @file Конфиг для запуска тестов на код с высоким приоритетом.
 */

'use strict';

const baseConfig = require('./baseConfig');

const matchPaths = [
    '<rootDir>/src/**/common/helpers/**',
    '<rootDir>/src/**/server/helpers/**',
    '<rootDir>/src/**/server/resolvers/**',
    '<rootDir>/src/**/server/rotators/**',
    '<rootDir>/src/**/server/selectors/**',
];

module.exports = {
    ...baseConfig,

    testMatch: matchPaths.map(path => `${path}/*.spec.{js,ts}`),

    collectCoverageFrom: matchPaths.map(path => `${path}/*.{js,ts}`),
};
