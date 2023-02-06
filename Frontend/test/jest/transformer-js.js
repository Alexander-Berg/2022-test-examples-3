const path = require('path');
const babelJest = require('babel-jest');
const { getReactLevels } = require('tools-access-react');

const { SRC_PATH } = require('../../.config/webpack/vars');

module.exports = babelJest.createTransformer({
    plugins: [
        [
            require.resolve('babel-plugin-bem-import'),
            {
                naming: 'origin',
                langs: ['ru'],
                techs: ['js'],
                techMap: {
                    js: ['react.js'],
                },
                levels: [
                    ...getReactLevels(),
                    path.join(SRC_PATH, 'abc/react/components'),
                ],
            },
        ],
        [
            require.resolve('babel-plugin-transform-es2015-modules-commonjs'),
        ],
    ],
});
