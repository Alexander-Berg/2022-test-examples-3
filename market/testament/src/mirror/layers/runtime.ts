import {CallOptions} from '../method';
import CallResult from '../result';

export interface RuntimeLayer {
    runCode<TArg extends readonly any[], TResult>(
        code: (...args: TArg) => TResult,
        args: TArg,
        filename?: string,
        callOptions?: CallOptions,
    ): Promise<CallResult<TResult>>;

    requireModule(moduleName: string): any;
}

export interface RuntimeBackend {
    runCode<TArg extends readonly any[], TResult>(
        code: string,
        args: TArg,
        filename?: string,
    ): TResult;

    requireModule(moduleName: string): any;
}
