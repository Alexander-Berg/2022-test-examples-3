'use strict';

module.exports = {
    verbose: true,
    // костыль для прогона тестов в сендбоксе
    testURL: 'http://localhost/',
    coveragePathIgnorePatterns: ['<rootDir>/node_modules/'],
    testPathIgnorePatterns: ['<rootDir>/node_modules/'],
    modulePaths: ['<rootDir>'],
};
