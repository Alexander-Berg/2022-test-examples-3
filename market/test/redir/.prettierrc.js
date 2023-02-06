/**
 * @see {@link https://prettier.io/docs/en/configuration.html}
 */
module.exports = {
    singleQuote: true,
    printWidth: 100,
    tabWidth: 2,

    overrides: [
        {
            files: ['*.json'],
            options: {
                parser: 'json',
            },
        },
        {
            files: ['*.js'],
            options: {
                parser: 'babel',
                tabWidth: 4,
                trailingComma: 'all',
                bracketSpacing: true,
                jsxBracketSameLine: false,
                arrowParens: 'always',
            },
        },
    ],
};
