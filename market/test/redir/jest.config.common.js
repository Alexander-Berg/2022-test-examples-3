/**
 * @see {@link https://jestjs.io/docs/en/configuration}
 */
module.exports = {
    collectCoverageFrom: ['middleware/**/*.js', 'utils/**/*.js', '!utils/create.js'],
    coverageThreshold: {
        global: {
            branches: 80,
            functions: 80,
            lines: 80,
            statements: 80,
        },
    },
};
