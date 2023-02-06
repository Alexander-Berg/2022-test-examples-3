/**
 * @file Конфиг для запуска всех тестов в проекте.
 */

'use strict';

const baseConfig = require('./baseConfig');

module.exports = {
    ...baseConfig,

    testMatch: ['<rootDir>/configs/**/*.spec.js', '<rootDir>/helpers/**/*.spec.js', '<rootDir>/src/**/*.spec.{js,ts}'],

    collectCoverageFrom: ['configs/**/*.js', 'helpers/**/*.js', 'src/**/*.{js,ts,tsx}'],
};
