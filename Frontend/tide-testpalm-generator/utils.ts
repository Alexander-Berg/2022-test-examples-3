import { inspect } from 'util';
import _ from 'lodash';

import { Tide } from '../../types';
import { PluginConfig } from './types';

export function getTestpalmFilePath(hermioneFilePath: string, tide: Tide): string {
    const { constants, fileCollection } = tide;
    const filePaths = fileCollection ? fileCollection.getMapping(hermioneFilePath) : [];

    if (filePaths && filePaths.length) {
        return filePaths[0];
    }

    if (!constants.testpalm || !constants.hermione) {
        return '';
    }

    const { FILE_EXTS: hermioneFileExts } = constants.hermione;
    const { FILE_EXTS: testpalmFileExts } = constants.testpalm;
    const hermioneFileExt = hermioneFileExts.find((fileExt) => hermioneFilePath.endsWith(fileExt));
    const testpalmFileExt = testpalmFileExts[0];

    return hermioneFilePath.replace(new RegExp(`${hermioneFileExt}$`), testpalmFileExt);
}

export function compileArg(
    arg: any,
    pluginConfig: PluginConfig,
    withTextReplace: boolean = false,
): any {
    if (!arg) {
        return arg;
    }

    if (_.isString(arg) && withTextReplace) {
        return pluginConfig.textReplacers[arg] || arg;
    }

    if (arg[Symbol.for('raw')]) {
        const value = arg.toString();

        if (withTextReplace) {
            return pluginConfig.textReplacers[value] || value;
        }

        return value;
    }

    if (_.isArray(arg)) {
        return arg.map((item) => compileArg(item, pluginConfig, withTextReplace));
    }

    if (typeof arg === 'object') {
        return _.keys(arg).reduce((acc, key) => {
            acc[key] = compileArg(arg[key], pluginConfig, withTextReplace);

            return acc;
        }, {});
    }

    return arg;
}

export function normalizeArg(arg: any, pluginConfig: PluginConfig): any {
    if (!arg) {
        return arg;
    }

    if (_.isString(arg)) {
        return `"${arg}"`;
    }

    if (typeof arg === 'object') {
        if (arg[Symbol.for('raw')]) {
            return arg.toString();
        }

        try {
            return inspect(compileArg(arg, pluginConfig), { compact: true });
        } catch (error) {
            console.log('\ncatch');
            console.log(error);
            console.log(JSON.parse(JSON.stringify(arg)));
        }
    }

    return arg.toString();
}
