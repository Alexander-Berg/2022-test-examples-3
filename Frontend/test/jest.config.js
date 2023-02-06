module.exports = {
    verbose: true,
    testMatch: [
        '<rootDir>/**/*.test.js',
        '<rootDir>/**/*.test.ts',
    ],
    rootDir: __dirname,
    moduleFileExtensions: [
        'js', 'json', 'ts',
    ],
    setupFilesAfterEnv: ['jest-expect-message'],
    transform: {
        '^.+\\.ts$': 'ts-jest',
    },
    reporters: [
        'default',
        ['../node_modules/jest-html-reporter', {
            pageTitle: 'Test Report',
            outputPath: 'unit-tests.html',
        }],
    ],
};
