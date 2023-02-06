import _ from 'lodash';
import { NodePath } from 'ast-types/lib/node-path';
import { TestdataManagerOptions, UrlPlain } from './types';
import { parseConfig, testdataSlowReplacer } from './options';
import { File, Test, TestdataFile, TestFile, TestFileParser, Tide } from '../..';
import { HermioneAstParser } from '../../types';
import { fromAst, toAst, walk, walkAndUnwrapJson } from './utils';
import { reportWarning } from '../../utils/logging';

export class TideTestdataManager {
    private _tide: Tide;
    private _pluginOptions: TestdataManagerOptions;

    constructor(tide: Tide, pluginOptions: Partial<TestdataManagerOptions>) {
        this._tide = tide;
        this._pluginOptions = parseConfig(pluginOptions);
    }

    updateUrl(
        search: Partial<UrlPlain>,
        replacement: Partial<UrlPlain>,
        file: File,
        titlePath?: Test['titlePath'],
    ): void {
        this._pluginOptions.oldUrl = search;
        this._pluginOptions.newUrl = replacement;

        switch (file.tool) {
            case 'hermione':
                return this.updateHermioneFile(search, replacement, file as TestFile, titlePath);
            case 'testpalm':
                return this.updateTestpalmFile(search, replacement, file as TestFile, titlePath);
            case 'testdata':
                return this.updateTestdataFile(search, replacement, file as TestdataFile);
        }
    }

    updateHermioneFile(
        search: Partial<UrlPlain>,
        replacement: Partial<UrlPlain>,
        file: TestFile,
        titlePath?: Test['titlePath'],
    ): void {
        const parser = this._tide.parsers.hermione as HermioneAstParser;
        const j = parser.parser;
        const commandNames = _.keys(this._pluginOptions.hermioneReplacers);
        const errors: Error[] = [];

        file.ast
            ?.find(j.CallExpression, (node) => {
                return (
                    j.MemberExpression.check(node.callee) &&
                    commandNames.includes(node.callee.name ?? node.callee.property.name)
                );
            })
            ?.forEach((path: NodePath) => {
                if (
                    titlePath &&
                    !TestFileParser.getFullTitle(titlePath).startsWith(parser.getFullTitle(path))
                ) {
                    return;
                }
                const commandName = parser.getCalleeName(path);
                const replacer = this._pluginOptions.hermioneReplacers[commandName];
                try {
                    const oldArgs = path.value.arguments.map((arg) => fromAst(arg, parser));
                    const newArgs = replacer(oldArgs, commandName, this._pluginOptions);

                    if (!_.isEqual(oldArgs, newArgs)) {
                        file.isModified = true;

                        for (let i = 0; i < path.value.arguments.length; i++) {
                            if (!_.isNil(newArgs[i])) {
                                path.value.arguments[i] = toAst(newArgs[i], parser);
                            }
                        }
                    }
                } catch (e) {
                    errors.push(e);
                }
            });
        if (errors.length) {
            reportWarning(
                this._tide,
                `Warning: some of the urls in ${
                    file.filePath ?? '<unknown-path>'
                } were not updated due to errors.`,
                errors,
            );
        }
    }

    updateTestpalmFile(
        search: Partial<UrlPlain>,
        replacement: Partial<UrlPlain>,
        file: TestFile,
        titlePath?: Test['titlePath'],
    ): void {
        if (!file.data) {
            return;
        }

        const targetProperties = _.keys(this._pluginOptions.testpalmReplacers);
        walk(file.data, (object, property, path): boolean => {
            if (targetProperties.includes(property)) {
                if (titlePath) {
                    const titlePathSlice = titlePath.slice(1);
                    const currentPath = path.slice(1).slice(0, titlePathSlice.length);
                    if (!_.isEqual(titlePathSlice, currentPath)) {
                        return false;
                    }
                }
                const replacer = this._pluginOptions.testpalmReplacers[property];
                const oldData = object[property];
                const newData = replacer(oldData, this._pluginOptions);

                if (!_.isEqual(oldData, newData)) {
                    file.isModified = true;
                    object[property] = newData;
                }
                return false;
            }
            return true;
        });
    }

    updateTestdataFile(
        search: Partial<UrlPlain>,
        replacement: Partial<UrlPlain>,
        file: TestdataFile,
    ): void {
        if (!file.contents) {
            throw new Error(
                `Can't update file ${
                    file.filePath ?? '<unknown-path>'
                } because its contents field is undefined.`,
            );
        }

        if (this._pluginOptions.useSlowAlgorithm) {
            walkAndUnwrapJson(
                file.data as Record<string, any>,
                (object, property, path): boolean => {
                    const oldData = object[property];

                    let newData = testdataSlowReplacer(
                        file.data as Record<string, any>,
                        path,
                        this._pluginOptions,
                    );

                    if (!_.isEqual(oldData, newData)) {
                        file.isModified = true;
                        object[property] = newData;
                    }
                    return true;
                },
            );
            file.contents = JSON.stringify(file.data);
        } else {
            for (const replacer of _.values(this._pluginOptions.testdataReplacers)) {
                file.contents = replacer(
                    this._pluginOptions.oldUrl as Partial<UrlPlain>,
                    this._pluginOptions.newUrl as Partial<UrlPlain>,
                    file.contents,
                );
            }
        }

        return;
    }
}
