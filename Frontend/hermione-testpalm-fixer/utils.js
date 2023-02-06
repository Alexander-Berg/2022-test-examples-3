'use strict';

const _ = require('lodash');
const fs = require('fs');
const path = require('path');
const util = require('util');
const j = require('jscodeshift');
const recast = require('recast');

const astTypes = recast.types;
const NodePath = astTypes.NodePath;

const exists = util.promisify(fs.exists);
const {
    YML_EXPECTED_FILE_EXTNAMES: EXTS,
    YML_IGNORE_KEYS,
    YML_SPECS_TYPE_KEYS,
    YML_SPECS_TITLE_KEYS
} = require('./constants');
const { createIndexSignature } = require('typescript');

const defaultArgv = {
    path: ['**/*.testpalm.yml'],
    chunkSize: 100,
    rewrite: true,
    soft: false,
    verbose: false
};

function parseArgv(processArgv) {
    return processArgv.reduce((argv, value, i) => {
        if (value === '--path') {
            const paths = [];

            processArgv.slice(i + 1).forEach(value => {
                if (!value.startsWith('-')) {
                    const preparedValue = path.relative(process.cwd(), path.resolve(value));

                    if (!preparedValue.includes('*') && fs.lstatSync(preparedValue).isDirectory()) {
                        paths.push(path.relative(process.cwd(), path.resolve(preparedValue, '*.testpalm.yml')));
                        paths.push(path.relative(process.cwd(), path.resolve(preparedValue, '**/*.testpalm.yml')));
                    } else {
                        paths.push(preparedValue);
                    }
                }
            });

            if (paths.length) {
                argv.path = paths;
            }
        }

        if (value === '--size') {
            const nextArgv = processArgv[i + 1];
            const numberArgv = Number(nextArgv);

            if (!nextArgv.startsWith('-') && Number.isSafeInteger(numberArgv) && numberArgv) {
                argv.chunkSize = numberArgv;
            }
        }

        if (value === '--soft') {
            argv.soft = true;
        }

        if (value === '--verbose') {
            argv.verbose = true;
        }

        if (value === '-n') {
            argv.rewrite = false;
        }

        return argv;
    }, defaultArgv);
}

async function getTestFilePathes(ymlFilePath, data) {
    const savedTestFilePathes = data && data.files && data.files.length ? data.files : [];

    const testDirName = path.dirname(ymlFilePath);
    const exts = ['.hermione.ts', '.hermione.e2e.ts', '.hermione.js', '.hermione.e2e.js'];

    const newTestFilePathes = exts.map(ext => {
        return path.join(testDirName, path.basename(ymlFilePath, '.testpalm.yml') + ext);
    });

    const allTestFilePathes = _.uniq([].concat(savedTestFilePathes, newTestFilePathes));

    return (await Promise.all(
        allTestFilePathes
            .filter(filePath => _.isString(filePath) && EXTS.includes(path.extname(filePath)))
            .map(async filePath => {
                return (await exists(filePath)) && filePath;
            }))
    )
        .filter(Boolean);
}

function getFullTitlesFromData(data, specsTypeKeys = YML_SPECS_TYPE_KEYS) {
    const getPathes = (data, toolKey) => {
        const specTitle = getSpecsTitle(data);
        const pathes = [];
        const traverse = (node, path = []) => {
            if (node && _.isObject(node) && !_.isArray(node)) {
                _.keys(node).forEach(key => {
                    if (!YML_IGNORE_KEYS.includes(key) && !key.startsWith('%%')) {
                        traverse(node[key], [...path, key]);
                    }
                });
            } else if (node && _.isArray(node)){
                pathes.push(path);
            }
        };

        traverse(data[toolKey], [specTitle]);

        return pathes;
    };

    return specsTypeKeys
        .map(key => getPathes(data, key))
        .reduce((acc, arr) => acc.concat(arr))
        .filter(Boolean)
        .map(specPath => specPath.join(' '));
}

function getSpecsTitle(obj) {
    return typeof obj === 'string' ? obj : YML_SPECS_TITLE_KEYS
        .map(prop => obj[prop])
        .filter(Boolean)
        .join(' / ');
}

function parseSpecsTitle(str) {
    const obj = str.split(' / ');
    const result = {};

    result.feature = _.get(obj, '0');

    if (_.get(obj, '1')) {
        result.type = _.get(obj, '1');
    }

    if (_.get(obj, '2')) {
        result.experiment = _.get(obj, '2');
    }

    return result;
}

function getFullTitlesFromCollection(testCollection) {
    if (!testCollection) {
        return [];
    }

    return _.uniq(testCollection.mapTests(test => test.fullTitle()));
}

function invertDict(dict) {
    return _.keys(dict).reduce((acc, key) => {
        dict[key].forEach(dublicateKey => acc[dublicateKey] = key);

        return acc;
    }, {});
}

async function getTestCollection(hermione, testPaths) {
    try {
        if (testPaths.length) {
            return await hermione.readTests(testPaths);
        }
    } catch (e) {}
}

function astReplace(node, ...args) {
    if (!node) {
        return;
    }

    const [path, keyName, replace, firstOnly] = args;

    if (node.type === 'ObjectExpression' && node.properties) {
        const props = keyName
            ? [_.find(node.properties, prop => _.get(prop, 'key.name') === keyName)]
            : node.properties;

        props.forEach(prop => astReplace(prop, ...args));
    } else if (node.type === 'Literal' && node.value) {
        _.set(node, 'value', replace(node.value));
    } else if (node.type === 'TemplateLiteral') {
        [].concat(node.quasis, node.expressions).forEach((element, i) => {
            if (i === 0 || !firstOnly) {
                astReplace(element, ...args);
            }
        });
    } else if (node.type === 'TemplateElement' && _.get(node, 'value.raw')) {
        _.set(node, 'value.raw', replace(_.get(node, 'value.raw')));
        _.set(node, 'value.cooked', replace(_.get(node, 'value.cooked')));
    } else if (node.type === 'Identifier') {
        const valueNodes = astFindValues(path, node);

        valueNodes.forEach(valueNode => astReplace(valueNode, ...args));
    } else if (node.elements) {
        node.elements.forEach(element => astReplace(element, ...args));
    } else if (node.value) {
        astReplace(node.value, ...args);
    }
}

function astFindValues(path, node) {
    // TODO: добавить варианты node.type ArrowFunctionExpression, FunctionExpression, CallExpression, MemberExpression
    if (!path || !node) {
        return [];
    }
    
    if (node.elements) {
        // TODO: добавить вариант, когда значение элемента массива может быть несколько (цикл, функция)
        const cloneNode = _.clone(node);

        cloneNode.elements = _.flattenDeep(node.elements.map(element => astFindValues(path, element)));

        return [cloneNode];
    }

    if (node.properties) {
        // TODO: добавить вариант, когда значение свойств объекта может быть несколько (цикл, функция)
        const cloneNode = _.clone(node);

        cloneNode.properties = _.flattenDeep(node.properties.map(property => {
            const cloneProperty = _.clone(property);

            cloneProperty.value = _.flattenDeep(astFindValues(path, property.value))[0];

            return cloneProperty;
        }));

        return [cloneNode];
    }

    if (node.type === 'MemberExpression') {
        // TODO: добавить вариант, когда значение свойств объекта может быть несколько (цикл, функция)
        // TODO: добавить вариант c CallExpression
        const objectValueNodes = astFindValues(path, node.object);
        const propertyValueNodes = node.computed ? astFindValues(path, node.property) : [node.property];

        return _.flattenDeep(
            objectValueNodes.map(node => {
                if (node.elements) {
                    return propertyValueNodes.map(prop => node.elements[prop.value]);
                } else if (node.properties) {
                    return propertyValueNodes.map(prop => {
                        const propIndex = _.findIndex(node.properties, { key: { name: prop.value || prop.name } });

                        return _.get(node, `properties.${propIndex}.value`);
                    });
                } else {
                    return propertyValueNodes.map(prop => node.value[prop.value]);
                }

            })
        );
    }

    if (node.type !== 'Identifier') {
        return [node];
    }

    const scopeAst = j(path).closestScope();
    const scopePath = scopeAst.paths()[0];
    const blockParentPath = _.get(j(path).closest(j.BlockStatement).paths(), '0.parent');

    const scopeDeclarationValueNodes = _.flattenDeep(
        scopeAst
            .findVariableDeclarators(node.name)
            .filter(path => {
                return blockParentPath
                    ? _.get(j(path).closest(j.BlockStatement).paths(), '0.parent') === blockParentPath
                    : _.get(j(path).closestScope().paths(), '0') === scopePath
            })
            .paths()
            .map(path => astFindValues(path, _.get(path, 'node.init')))
    )
        .filter(Boolean);

    if (scopeDeclarationValueNodes.length) {
        return scopeDeclarationValueNodes;
    }

    const paramValueNodes = _.flattenDeep(
        scopeAst
            .paths()
            .filter(path => _.get(path, 'node.params.length'))
            .map(path => {
                // TODO: добавить вариант c обычным CallExpression
                const paramNode = _.get(path, 'node.params.0');
                const objectNode = _.get(path, 'parent.node.callee.object');

                if (!(paramNode || objectNode)) {
                    return;
                }

                const objectValueNodes = astFindValues(path.parent, objectNode);

                if (paramNode.name === node.name) {
                    return objectValueNodes;
                } else if ((paramNode.properties || []).some(prop => prop.key.name === node.name)) {
                    const paramPropIndex = _.findIndex(paramNode.properties, { key: { name: node.name } });

                    return _.flattenDeep(objectValueNodes.map(node => {
                        return node.elements.map(element => {
                            return _.get(element, `properties.${paramPropIndex}.value`)
                        });
                    }));
                }
            })
    )
        .filter(Boolean);

    if (paramValueNodes.length) {
        return paramValueNodes;
    }

    return astFindValues(_.get(scopePath, 'parent'), node);
}

module.exports = {
    parseArgv,
    getTestFilePathes,
    getFullTitlesFromData,
    getFullTitlesFromCollection,
    getTestCollection,
    getSpecsTitle,
    parseSpecsTitle,
    invertDict,
    astReplace
};
