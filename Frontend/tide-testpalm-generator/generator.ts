import _ from 'lodash';
import { Collection } from 'jscodeshift';
import { NodePath } from 'ast-types/lib/node-path';
import { ASTPath } from 'jscodeshift/src/core';

import { TestFile, TestFileParser, HermioneAstParser } from '../..';
import { executeReplacer } from './replacer';
import { compileArg, getTestpalmFilePath, normalizeArg } from './utils';
import { Test, Tide } from '../../types';
import { PluginConfig, TestCommand } from './types';

export default class TestpalmGenerator {
    private _tide: Tide;
    private _pluginConfig: PluginConfig;
    private _parser: HermioneAstParser;
    private _ignoreObjectsDict: Record<string, boolean>;
    private _replacersDict: Record<string, number>;

    constructor(tide: Tide, pluginConfig: PluginConfig) {
        this._tide = tide;
        this._pluginConfig = pluginConfig;
        this._parser = tide.parsers.hermione as HermioneAstParser;
        this._ignoreObjectsDict = this._pluginConfig.ignoreObjects.reduce((acc, key) => {
            acc[key] = true;

            return acc;
        }, {});
        this._replacersDict = _.keys(this._pluginConfig.replacers).reduce((acc, key) => {
            const value = this._pluginConfig.replacers[key];

            if (!_.isFunction(value) && _.isObject(value)) {
                _.keys(this._pluginConfig.replacers[key]).forEach((subkey) => {
                    acc[`${key}.${subkey}`] = 1;
                });

                return acc;
            }

            const multiKey = key.split(',');

            acc[multiKey[0]] =
                !acc[multiKey[0]] || multiKey.length > acc[multiKey[0]]
                    ? multiKey.length
                    : acc[multiKey[0]];

            return acc;
        }, {});
    }

    getTestpalmFile(hermioneFile: TestFile): TestFile | {} {
        const { constants, fileCollection } = this._tide;

        if (!constants.testpalm) {
            return {};
        }

        const tool = 'testpalm';
        const fileExt = constants.testpalm.FILE_EXTS[0];
        const filePath = getTestpalmFilePath(hermioneFile.filePath as string, this._tide);
        let testpalmFile = fileCollection.getFile(filePath);

        if (!testpalmFile) {
            testpalmFile = new TestFile({ tool, filePath, fileExt, data: {} });
            fileCollection.addFile(testpalmFile);
            hermioneFile.tests.forEach((test) =>
                test.update({ tool, filePath, files: testpalmFile as TestFile }),
            );
        }

        fileCollection.addMapping(hermioneFile.filePath as string, testpalmFile.filePath as string);

        return testpalmFile;
    }

    generateData(testFile: TestFile, data = {}): Record<string, any> {
        const { constants } = this._tide;

        if (!constants.testpalm) {
            return {};
        }

        const { SPECS_TYPE_KEYS, META_REQUIRED_KEYS } = constants.testpalm;
        const specsKey = SPECS_TYPE_KEYS[testFile.type as 'integration' | 'e2e'];
        const meta = this.generateMeta(testFile);

        const specs = testFile.tests.reduce((acc, test) => {
            const testSpecs = this.generateTestSpecs(testFile, test);

            return _.merge(acc, testSpecs);
        }, {});

        const newData: Record<string, any> = {
            ...meta,
            [specsKey]: specs,
            files: [testFile.filePath],
        };

        if (_.isEqual(data, {})) {
            META_REQUIRED_KEYS.forEach((key: string) => (newData[key] = newData[key] || null));
        }

        return newData;
    }

    // TODO: https://st.yandex-team.ru/FAT-39
    mergeData(oldData = {}, newData: Record<string, any>): Record<string, any> {
        const { constants } = this._tide;

        if (!constants.testpalm) {
            return {};
        }

        const { ORDER_KEYS } = constants.testpalm;
        const result = _.mergeWith({}, oldData, newData, (oldValue, newValue, key) => {
            if (key === 'files') {
                return _.uniq([].concat(newValue, oldValue).filter(Boolean));
            }

            if (_.isArray(newValue) && typeof newValue[0] === 'object') {
                if (this._pluginConfig.rewrite) {
                    return newValue;
                }

                // возвращаем список шагов и не мержим их дальше
                return oldValue;
            }

            if (!oldValue && newValue !== undefined) {
                return newValue;
            }
        });
        const resultKeys = _.keys(result);
        const orderWithCommentKeys = ORDER_KEYS.reduce((acc: string[], key, i) => {
            const resultKeyIndex = resultKeys.findIndex((resultKey) => resultKey === key);

            if (resultKeyIndex !== -1) {
                if (resultKeys[i - 1] && resultKeys[i - 1].startsWith('%%COMMENT')) {
                    acc.push(resultKeys[i - 1]);
                }

                acc.push(key);
            }

            return acc;
        }, []);

        const otherKeys = _.difference(resultKeys, orderWithCommentKeys);

        return [...orderWithCommentKeys, ...otherKeys].reduce(
            (acc: { [key: string]: any }, key) => {
                if (result[key] !== undefined) {
                    acc[key] = result[key];
                }

                return acc;
            },
            {},
        );
    }

    generateMeta(testFile: TestFile): Record<string, string> {
        if (testFile.data) {
            return this.generateMetaFromData(testFile);
        }

        return this.generateMetaFromAst(testFile);
    }

    generateTestSpecs(testFile: TestFile, test: Test): Record<string, any> {
        if (testFile.data) {
            return this.generateTestSpecsFromData(testFile, test);
        }

        return this.generateTestSpecsFromAst(testFile, test);
    }

    generateMetaFromData(testFile: TestFile): Record<string, string> {
        const { constants } = this._tide;

        if (!constants.testpalm || !constants.hermione) {
            return {};
        }

        const { SPECS_TYPE_KEYS } = constants.testpalm;

        return _.omit(testFile.data, _.values(SPECS_TYPE_KEYS));
    }

    generateMetaFromAst(testFile: TestFile): Record<string, string> {
        const { constants } = this._tide;
        const j = this._parser.parser;

        if (!constants.testpalm || !constants.hermione) {
            return {};
        }

        const { ast, filePath } = testFile;
        const { TITLE_KEYS } = constants.testpalm;
        const { AST_CALLEE_NAMES } = constants.hermione;
        let meta: Record<string, any> = {};

        if (!ast) return meta;

        ast.find(j.CallExpression, (node: any) => {
            const calleeName = _.get(node, 'callee.name');

            return _.values(AST_CALLEE_NAMES).includes(calleeName);
        })
            .filter((path: NodePath) => path.scope.isGlobal)
            .at(0)
            .forEach((path: NodePath) => {
                const node = _.get(path, 'node.arguments[0]');

                if (node.type === 'ObjectExpression') {
                    const data = this._parser.getParsedValue(node, filePath as string);

                    meta = TITLE_KEYS.reduce((acc: Record<string, any>, key: string) => {
                        if (data[key]) {
                            acc[key] = data[key];
                        }

                        return acc;
                    }, {});
                } else {
                    meta.feature = node.value;
                }
            });

        return meta;
    }

    generateTestSpecsFromData(testFile: TestFile, test: Test): Record<string, any> {
        const { constants } = this._tide;

        if (!constants.hermione || !constants.testpalm) {
            return {};
        }

        const { SPECS_TYPE_KEYS } = constants.testpalm;
        const testSpecKey = SPECS_TYPE_KEYS[test.type];
        const { data, filePath } = testFile;

        let specs = {};

        if (!data) {
            return specs;
        }

        const traverse = (
            data: Record<string, any> | Record<string, any>[],
            titlePath: string[],
        ): void => {
            if (_.isArray(data)) {
                const testStepList = this.generateTestStepsFromData(data, filePath as string);

                _.defaultsDeep(specs, _.set({}, titlePath, testStepList));
            } else if (data) {
                _.defaultsDeep(specs, _.set({}, titlePath, {}));

                _.keys(data).forEach((titlePart) => {
                    traverse(data[titlePart], [...titlePath, titlePart]);
                });
            }
        };

        traverse(data[testSpecKey], []);

        return specs;
    }

    generateTestSpecsFromAst(testFile: TestFile, test: Test): Record<string, any> {
        const { constants } = this._tide;
        const j = this._parser.parser;

        if (!constants.hermione) {
            return {};
        }

        const { ast, filePath } = testFile;
        const { AST_CALLEE_NAMES, AST_HOOK_NAMES, TITLE_KEYS } = constants.hermione;
        const targetTitle = test.fullTitle();
        const titlePath = TestFileParser.getStringTitleParts(test.titlePath);
        let index = titlePath.length - 1;

        let specs = {};

        if (!ast) {
            return specs;
        }

        const traverse = (path: NodePath): void => {
            if (_.get(path, 'value.type') === 'CallExpression') {
                const calleeName = this._parser.getCalleeName(path);

                if (_.values(AST_CALLEE_NAMES).includes(calleeName)) {
                    const firstArg = _.get(path, 'value.arguments.0') || {};

                    const title =
                        calleeName === AST_CALLEE_NAMES.SPECS &&
                        firstArg.type === 'ObjectExpression'
                            ? HermioneAstParser.getTitleFromObject(
                                  this._parser.getParsedValue(firstArg, filePath),
                                  TITLE_KEYS,
                              )
                            : firstArg.value;

                    if (title === titlePath[index]) {
                        const specsPath = TestFileParser.getStringTitleParts(
                            titlePath.slice(1, index + 1),
                        );
                        const ast = j(path);

                        if (calleeName === AST_CALLEE_NAMES.IT) {
                            const it = this.generateTestStepListFromAst(ast, filePath as string);

                            _.set(specs, specsPath, it);
                        }

                        _.values(AST_HOOK_NAMES).forEach((hookType) => {
                            const hook = this.generateHookStepListFromAst(
                                ast,
                                hookType,
                                filePath as string,
                            );

                            if (hook) {
                                if (hookType === AST_HOOK_NAMES.BEFORE_EACH) {
                                    specs = _.merge(
                                        _.set({}, [...specsPath, hookType], hook),
                                        specs,
                                    );
                                } else {
                                    _.set(specs, [...specsPath, hookType], hook);
                                }
                            }
                        });

                        index--;
                    }
                }
            }

            if (path.parentPath) {
                traverse(path.parentPath);
            }
        };

        ast.find(j.CallExpression)
            .filter((path: NodePath) => {
                const calleeName = this._parser.getCalleeName(path);

                return _.values(AST_CALLEE_NAMES).includes(calleeName);
            })
            .forEach((path: NodePath) => {
                const currentTitle = this._parser.getFullTitle(path, filePath as string);

                if (currentTitle === targetTitle) {
                    traverse(path);
                }
            });

        return specs;
    }

    generateHookStepListFromAst(
        ast: Collection,
        hookType: string,
        filePath: string,
    ): undefined | Record<string, string>[] {
        const j = this._tide.parsers.hermione.parser;

        let hooks;

        const currentRootPath = ast.getAST()[0];

        ast.find(j.CallExpression)
            .filter((path) => this._parser.getCalleeName(path) === hookType)
            .filter(
                (path) =>
                    j(path)
                        .closest(j.CallExpression)
                        .filter((path) => path === currentRootPath)
                        .size() > 0,
            )
            .at(0)
            .forEach((path) => {
                hooks = this.generateTestStepListFromAst(j(path), filePath);
            });

        return hooks;
    }

    generateTestStepListFromAst(ast: Collection, filePath: string): Record<string, string>[] {
        const { replacers } = this._pluginConfig;
        const j = this._tide.parsers.hermione.parser;
        const testStepList: Array<any> = [];
        const visited = new Set<any>();

        function isSomePathParentVisited(path: ASTPath<unknown>, visited: Set<any>): boolean {
            let currentPath = path;
            while (currentPath) {
                if (visited.has(currentPath)) {
                    return true;
                }
                currentPath = currentPath.parentPath;
            }
            return false;
        }

        ast.find(j.Node).forEach((path): void => {
            if (isSomePathParentVisited(path, visited)) {
                return;
            }
            const isCallExpressionWithoutObject =
                j.CallExpression.check(path.node) &&
                j.Identifier.check(_.get(path, 'node.callee')) &&
                replacers[_.get(path, 'node.callee.name')];

            const isMemberExpression = j.MemberExpression.check(path.node);

            const isArgumentOfCallExpression =
                path.parentPath.name === 'arguments' &&
                ['argument', 'object'].includes(path.parent.name);

            let callPaths;
            let replacerObject: string | undefined;

            if (isCallExpressionWithoutObject) {
                callPaths = [path];
            }

            if (isMemberExpression) {
                replacerObject = _.keys(replacers).find(
                    (name) =>
                        _.get(path, 'node.object.name') === name ||
                        _.get(path, 'node.property.name') === name,
                );

                if (replacerObject && typeof replacers[replacerObject] !== 'function') {
                    callPaths = this._parser.getMemberCallExpressionsPaths(path);
                }
            }

            if (callPaths && callPaths.length) {
                callPaths.forEach((path: NodePath) => {
                    const testSteps = this.generateTestStepsFromAst(path, {
                        replacerObject,
                        filePath,
                    });

                    testStepList.push(testSteps);
                });

                visited.add(path);
            }

            if (isArgumentOfCallExpression) {
                // TODO: нужно научиться вытаскивать вызовы команд внутри других команд в правильном порядке
                visited.add(path);
            }
        });

        return _.flattenDeep(testStepList).filter(Boolean);
    }

    generateTestStepsFromData(steps: Record<string, any>[], filePath: string): object | object[] {
        const { replacers } = this._pluginConfig;
        const commands = steps.map((step) => this.prepareCommandFromData(step));
        const testStepList: object[] = [];

        for (let i = 0; i < commands.length; i++) {
            const command = commands[i];
            const groupCount = this._replacersDict[command.path];
            let replacer: Function | null | undefined;
            let replacingCommands = [command];

            if (groupCount > 1) {
                for (let j = groupCount; j > 1; j--) {
                    replacingCommands = commands.slice(i, i + j);

                    const joinedPath = replacingCommands.map((step) => step.path).join(',');

                    replacer = _.get(replacers, [joinedPath]);

                    if (replacer) {
                        i = i + j - 1;

                        break;
                    }
                }
            } else {
                replacer = _.get(replacers, command.path) || _.get(replacers, [command.path]);
            }

            if (!(this._ignoreObjectsDict[_.get(replacingCommands, '0.object')] && !replacer)) {
                testStepList.push(executeReplacer(replacingCommands, replacer, filePath));
            }
        }

        return _.flattenDeep(testStepList).filter(Boolean);
    }

    prepareCommandFromData(step: Record<string, any>): TestCommand {
        const codePath = (step.path || []).join('.');
        const normalizedArgs = step.arguments.map((arg: any) =>
            normalizeArg(arg, this._pluginConfig),
        );
        const compiledArgs = step.arguments.map((arg: any) =>
            compileArg(arg, this._pluginConfig, !_.isEmpty(this._pluginConfig.textReplacers)),
        );

        // TODO: поправить случаи, когда в code пустая строка, generator преваращает две ' в "
        const rawCode = `${codePath}(${normalizedArgs.join(', ')})`;
        const code = this._parser.normalizeRaw(rawCode);
        const command = {
            object: step.object,
            name: step.name,
            code,
            arguments: compiledArgs,
            path: codePath,
        };

        return command;
    }

    generateTestStepsFromAst(
        path: NodePath,
        {
            replacerObject,
            filePath,
        }: {
            replacerObject?: string;
            filePath: string;
        },
    ): object | object[] {
        const { replacers } = this._pluginConfig;
        const calleeName = this._parser.getCalleeName(path);
        const normalizedArgs = _.get(path, 'node.arguments').map((arg: any) =>
            this._parser.normalizeNode(arg),
        );
        const pathCode = [replacerObject as string, calleeName].filter(Boolean).join('.');
        const rawCode = `${pathCode}(${normalizedArgs.join(', ')})`;
        const code = this._parser.normalizeRaw(rawCode);
        const compiledArgs = normalizedArgs.map((arg: string) =>
            this._parser.compile(arg, filePath),
        );
        const command = {
            object: replacerObject,
            name: calleeName,
            code,
            arguments: compiledArgs,
            path: pathCode,
        };
        const replacer = _.get(replacers, pathCode) || _.get(replacers, [pathCode]);

        return executeReplacer(command, replacer, filePath);
    }
}
