/* eslint-disable @typescript-eslint/ban-types */

import Module from 'module';
import path from 'path';

import Layer from '../../layer';
import type {CallOptions} from '../../method';
import CallResult from '../../result';
import {resolveScriptPath} from '../../../utils/relativePath';
import {JestWorker} from './worker';
import PackedFunction from '../../packedFunction';
import makeMethods, {Methods} from './methods';

import MockOptions = jest.MockOptions;

export type AnyFn = (...args: any[]) => any;

// fixme: грязный хак, пока в маркетфронте нет нармального менеджера пакетов
const requireFromJest = Module.createRequire(require.resolve('jest'));
const {buildArgv} = requireFromJest('jest-cli/build/cli');

export default class JestLayer extends Layer<Methods, JestWorker> {
    static ID = 'jest';

    #testFilename: string;

    #jest: typeof jest;

    #jestRequire: AnyFn;

    constructor(testFilename: string, jestGlobal: typeof jest) {
        super(JestLayer.ID, resolveScriptPath(__filename, './worker.js'));
        this.#testFilename = testFilename;
        this.#jest = jestGlobal;
        this.#jestRequire = Module.createRequire(this.#testFilename);
    }

    getMethods(): Methods {
        return makeMethods(this);
    }

    getTestFilename(): string {
        return this.#testFilename;
    }

    async init(): Promise<void> {
        await this.worker.init(
            this.#testFilename,
            // @ts-ignore
            global.jestEnvironmentOptions.configPath,
            // @ts-ignore
            buildArgv(global.jestEnvironmentOptions.argv),
        );
    }

    async mock(
        moduleName: string,
        moduleFactory: AnyFn | PackedFunction<any, any>,
        options?: MockOptions,
        callOptions?: CallOptions,
    ): Promise<void> {
        console.warn('Please, use jestLayer.doMock instead of jestLayer.mock');
        await this.doMock(moduleName, moduleFactory, options, callOptions);
    }

    async doMock(
        moduleName: string,
        moduleFactory: AnyFn | PackedFunction<any, any>,
        options?: MockOptions,
        callOptions?: CallOptions,
    ): Promise<void> {
        await this.methods.doMock.call(
            [moduleName, moduleFactory, options || {}],
            callOptions,
        );
    }

    async requireModule(
        moduleName: string,
        callOptions?: CallOptions,
    ): Promise<CallResult<any>> {
        return this.methods.requireModule.call([moduleName], callOptions);
    }

    async runCode<TArg extends readonly any[], TResult>(
        code: (...args: TArg) => TResult,
        args: TArg,
        filename?: string,
        callOptions?: CallOptions,
    ): Promise<CallResult<TResult>> {
        return this.methods.runCode.call([code, args], callOptions);
    }

    callFunction<TArg extends readonly any[], TResult>(
        code: (...args: TArg) => TResult,
        args: TArg,
    ): TResult {
        // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
        const jestGlobalArgs = require('@jest/globals');
        const module = {exports: {}};
        const parameters = {
            module,
            exports: module.exports,
            require: this.#jestRequire,
            __dirname: path.dirname(this.#testFilename),
            __filename: this.#testFilename,
            global,
            _getJestObj: () => this.#jest,
            ...jestGlobalArgs,
            jest: this.#jest,
        };
        // eslint-disable-next-line no-new-func
        const fn = new Function(
            Object.keys(parameters).join(','),
            `return (${code.toString()})(...${JSON.stringify(args)})`,
        ) as (...args: any) => TResult; // eslint-disable-line no-shadow
        return fn(...Object.values(parameters));
    }
}
