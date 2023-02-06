/* eslint-disable no-shadow */

import path from 'path';
import {Script} from 'vm';

import {Config} from '@jest/types';
import NodeEnvironment from 'jest-environment-node';

import createRuntimeDescriptor from './createRuntimeDescriptor';
import Runtime from '../../../runtime';

import MockOptions = jest.MockOptions;

export class JestWorker {
    #runtime: Runtime | null = null;

    #environment: NodeEnvironment | null = null;

    #testFilename: string | null = null;

    #config: Config.ProjectConfig | null = null;

    // eslint-disable-next-line @typescript-eslint/ban-types
    #jest: typeof jest | null = null;

    #jestRequire: ((moduleName: string) => any) | null = null;

    async init(
        testFilename: string,
        packageRootOrConfig: Config.Path,
        argv: Config.Argv,
    ): Promise<void> {
        this.#testFilename = testFilename;
        const {environment, runtime} = await createRuntimeDescriptor(
            this.#testFilename,
            packageRootOrConfig,
            argv,
        );
        this.#jestRequire = runtime.requireModuleOrMock.bind(
            runtime,
            this.#testFilename,
        );
        this.#environment = environment;
        this.#runtime = runtime;

        // @ts-ignore
        this.#jest = runtime.jestObjectCaches.get(this.#testFilename);

        if (!this.#jest) {
            // @ts-ignore
            this.#jest = runtime._createJestObjectFor(this.#testFilename);
            // @ts-ignore
            runtime.jestObjectCaches.set(this.#testFilename, this.#jest);
        }
    }

    getTestFilename(): string | null {
        return this.#testFilename;
    }

    doMock(
        moduleName: string,
        moduleFactory: string,
        options: MockOptions,
    ): void {
        if (!this.#testFilename) {
            throw new Error('No test filename');
        }

        this.#runtime?.setMock(
            this.#testFilename,
            moduleName,
            () => this.runCode(moduleFactory, []),
            options,
        );
    }

    requireModule(moduleName: string): any {
        return this.#runtime?.requireModule(moduleName);
    }

    compileCode<TResult>(
        code: string,
        args: readonly any[],
        filename = '<runCode script>',
    ): {parameters: Record<string, any>; fn: (...args: any[]) => TResult} {
        if (!this.#testFilename) {
            throw new Error('No test filename');
        }

        const jestGlobalArgs = this.#runtime?.requireModuleOrMock(
            this.#testFilename,
            '@jest/globals',
        ) as {[key: string]: any};
        const module = {exports: {}};
        const parameters = {
            module,
            exports: module.exports,
            require: this.#runtime?.requireModuleOrMock.bind(
                this.#runtime,
                this.#testFilename,
            ),
            __dirname: path.dirname(this.#testFilename),
            __filename: this.#testFilename,
            global: this.#environment?.global,
            _getJestObj: () => this.#jest,
            // @ts-ignore
            getBackend: global.getBackend,
            ...jestGlobalArgs,
            jest: this.#jest,
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

        const vmContext = this.#environment?.getVmContext();
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        const compiled = script.runInContext(vmContext!, {
            filename: filename || 'unknown',
        });

        return {parameters, fn: compiled};
    }

    callFunction<TArg extends readonly any[], TResult>(
        code: (...args: TArg) => TResult,
        args: TArg,
    ): TResult {
        return this.runCode<TResult>(code.toString(), args);
    }

    runCode<TResult>(
        code: string,
        args: readonly any[],
        filename = '<runCode script>',
    ): TResult {
        const compiled = this.compileCode<TResult>(code, args, filename);
        return compiled.fn.call(null, ...Object.values(compiled.parameters));
    }
}

export default new JestWorker();
