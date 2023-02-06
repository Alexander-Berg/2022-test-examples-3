import { existsSync, readdirSync, statSync } from 'fs';
import { join } from 'path';

import combinatorics from 'js-combinatorics';
import { camelCase, cloneDeep, fill, upperFirst } from 'lodash';

export interface IModule {
    enabled?: boolean;
    name: string;
    additionalFiles: string[];
    additionalPrompts: {
        [key: string]: string | boolean
    }
}

function listModuleFiles(templatesName: string): string[] {
    const basePath = join(__dirname, '../generators/app/templates', templatesName);

    if (!existsSync(basePath)) {
        return [];
    }

    function walk(dir: string): string[] {
        const files: string[] = [];

        for (const name of readdirSync(dir)) {
            const subname = `${dir}/${name}`;

            files.push(
                ...statSync(subname).isDirectory() ?
                    walk(subname) :
                    [subname.replace(basePath, '')]
            );
        }

        return files;
    }

    return walk(basePath);
}

function createModule(name: string, additionalPrompts: IModule['additionalPrompts']): IModule {
    return {
        name: upperFirst(camelCase(name)),
        additionalFiles: listModuleFiles(name),
        additionalPrompts,
    };
}

export const modules: IModule[] = [
    createModule('redux-saga', {
        shouldAddReduxSaga: true,
    }),
    createModule('sentry', {
        shouldAddSentry: true,
    }),
    createModule('tanker', {
        shouldAddTanker: true,
        tankerProject: 'project-stub',
    }),
    createModule('bunker', {
        shouldAddBunker: true,
        bunkerProject: 'project-stub',
    }),
];

export const modulesCombinations: IModule[][] = combinatorics
    .cartesianProduct(...fill(Array(modules.length), [true, false]))
    .toArray()
    .map(config => config.reduce((modulesConfig, isEnabled, index) => {
        modulesConfig[index].enabled = isEnabled;

        return modulesConfig;
    }, cloneDeep(modules)));
