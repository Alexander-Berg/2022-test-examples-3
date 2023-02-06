import path from 'path';
import Module from 'module';

import Method, {CallOptions} from '../../method';
import Layer from '../../layer';
import {AnyFn} from '../jest';
import CallResult from '../../result';
import {resolveScriptPath} from '../../../utils/relativePath';
import {RuntimeLayer} from '../runtime';
import {NativeWorker} from './worker';

export type Methods = {
    requireModule: Method<[path: string], any>;
    runCode: Method<[fn: AnyFn, args: readonly any[]], any>;
};

export default class NativeRuntime
    extends Layer<Methods, NativeWorker>
    implements RuntimeLayer
{
    static ID = 'nativeRuntime';

    #scriptFilename: string;

    #nativeRequire: (moduleName: string) => any;

    constructor(scriptFilename: string) {
        super(NativeRuntime.ID, resolveScriptPath(__filename, './worker.js'));
        this.#scriptFilename = scriptFilename;
        this.#nativeRequire = Module.createRequire(this.#scriptFilename);
    }

    getMethods(): Methods {
        return {
            requireModule: new Method(
                'requireModule',
                (moduleName: string) => this.#nativeRequire(moduleName),
                moduleName => this.worker.requireModule(moduleName),
            ),

            runCode: new Method(
                'runCode',
                (code: AnyFn, args: readonly any[]) =>
                    this.callFunction(code, args as any[]),
                (code, args) => this.worker.runCode(code.toString(), args),
            ),
        };
    }

    async init(): Promise<void> {
        await this.worker.init(this.#scriptFilename);
    }

    callFunction<TArg extends readonly any[], TResult>(
        code: (...args: TArg) => TResult,
        args?: TArg,
    ): TResult {
        // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
        const module = {exports: {}};
        const parameters = {
            module,
            exports: module.exports,
            require: this.#nativeRequire,
            __dirname: path.dirname(this.#scriptFilename),
            __filename: this.#scriptFilename,
            global,
        };
        // eslint-disable-next-line no-new-func
        const fn = new Function(
            Object.keys(parameters).join(','),
            `return (${code.toString()})(...${JSON.stringify(args)})`,
        ) as (...args: any) => TResult; // eslint-disable-line no-shadow
        return fn(...Object.values(parameters));
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
}
