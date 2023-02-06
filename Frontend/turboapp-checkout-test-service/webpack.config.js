const path = require('path');
const { DuplicatePackageCheckerPlugin } = require('@yandex-int/frontend-webpack-plugins');

const getLocalModule = name => path.join(__dirname, 'node_modules', name);

// При сборке проекта babel делает импорты хелперов по абсолютным путям до @babel/runtime
const babelImportPaths = [
    '@babel/runtime',
    path.join(__dirname, 'node_modules/babel-preset-react-app/node_modules/@babel/runtime'),
    path.join(__dirname, '../../packages/tap-scripts/node_modules/babel-preset-react-app/node_modules/@babel/runtime'),
];

/*
 * Этот конфиг расширяет @yandex-int/tap-scripts/config/webpack.config.js (конфиг CRA)
 */
module.exports = () => {
    return {
        resolve: {
            alias: {
                /**
                 * https://github.com/facebook/react/issues/13991#issuecomment-435587809
                 */
                react: getLocalModule('react'),
                'react-dom': getLocalModule('react-dom'),
                'react-router-dom': getLocalModule('react-router-dom'),
                'prop-types': getLocalModule('prop-types'),
                '@bem-react/core': getLocalModule('@bem-react/core'),
                '@bem-react/classname': getLocalModule('@bem-react/classname'),
                '@bem-react/classnames': getLocalModule('@bem-react/classnames'),
                isarray: getLocalModule('isarray'),
                ...babelImportPaths.reduce(
                    (paths, importPath) => ({ ...paths, [importPath]: getLocalModule('@babel/runtime') }),
                    {}
                ),
            },
        },
        plugins: [
            new DuplicatePackageCheckerPlugin({
                exclude(instance) {
                    // tslib приезжает из @yandex-lego и /packages/tap-js-api
                    // стреляет только при локальной сборке
                    return instance.package.name === 'tslib';
                },
            }),
        ],
    };
};
