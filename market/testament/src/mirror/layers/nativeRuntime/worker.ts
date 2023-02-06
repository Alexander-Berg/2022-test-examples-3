/* eslint-disable no-shadow */

import path from 'path';
import {Script} from 'vm';
import Module from 'module';

import {RuntimeBackend} from '../runtime';

export class NativeWorker implements RuntimeBackend {
    #scriptFilename: string | null = null;

    #nativeRequire: ((moduleName: string) => any) | null = null;

    async init(scriptFilename: string): Promise<void> {
        this.#scriptFilename = scriptFilename;
        this.#nativeRequire = Module.createRequire(this.#scriptFilename);
    }

    getTestFilename(): string | null {
        return this.#scriptFilename;
    }

    requireModule(moduleName: string): any {
        return this.#nativeRequire?.(moduleName);
    }

    callFunction<TArg extends readonly any[], TResult>(
        code: (...args: TArg) => TResult,
        args: TArg,
    ): TResult {
        return this.runCode<TArg, TResult>(code.toString(), args);
    }

    runCode<TArgs extends readonly any[], TResult>(
        code: string,
        args: TArgs,
        filename = '<runCode script>',
    ): TResult {
        if (!this.#scriptFilename) {
            throw new Error('No script filename');
        }

        const module = {exports: {}};
        const parameters = {
            module,
            exports: module.exports,
            require: this.#nativeRequire,
            __dirname: path.dirname(this.#scriptFilename),
            __filename: this.#scriptFilename,
            global,
            // @ts-ignore
            getBackend: global.getBackend,
        };
        const script = new Script(
            `
(${Object.keys(parameters).join(',')}) => {
  global.getBackend = getBackend;
  return (${code})(...${JSON.stringify(args)});
}`,
            {
                displayErrors: true,
                filename,
            },
        );

        const compiled = script.runInNewContext(
            {},
            {filename: filename || 'unknown'},
        );

        return compiled(...Object.values(parameters));
    }
}

export default new NativeWorker();
