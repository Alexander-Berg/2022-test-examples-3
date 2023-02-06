import CallResult from './result';

export type CallOptions = {
    mode?: 'client' | 'backend' | 'both';
};

export type MethodFunction<TArgs extends readonly any[], TReturn> = (
    ...args: TArgs
) => Promise<TReturn>;

export type ExtractMethodData<TMethod extends Method<any, any, any>> =
    TMethod extends Method<
        infer TArgs,
        infer TClientReturn,
        infer TBackendReturn
    >
        ? {
              args: TArgs;
              clientReturn: TClientReturn;
              backendReturn: TBackendReturn;
          }
        : never;

export type ExtractMethods<
    TMethods extends Record<string, Method<any, any>>,
    TType extends 'client' | 'backend',
> = {
    [N in keyof TMethods]: (
        ...args: ExtractMethodData<TMethods[N]>['args']
    ) => ExtractMethodData<TMethods[N]>[TType extends 'client'
        ? 'clientReturn'
        : 'backendReturn'];
};

export default class Method<
    TArgs extends readonly any[],
    TClientReturn,
    TBackendReturn = TClientReturn,
> {
    #name: string;

    #clientFn: MethodFunction<TArgs, TClientReturn>;

    #backendFn: MethodFunction<TArgs, TBackendReturn>;

    constructor(
        name: string,
        callClient: MethodFunction<TArgs, TClientReturn>,
        callBackend: MethodFunction<TArgs, TBackendReturn>,
    ) {
        this.#name = name;
        this.#clientFn = callClient;
        this.#backendFn = callBackend;
    }

    getName(): string {
        return this.#name;
    }

    callClient(args: TArgs): Promise<TClientReturn> {
        return this.#clientFn.call(null, ...args);
    }

    callBackend(args: TArgs): Promise<TBackendReturn> {
        return this.#backendFn.call(null, ...args);
    }

    async call(
        args: TArgs,
        callOptions: CallOptions = {},
    ): Promise<CallResult<TClientReturn, TBackendReturn>> {
        const result = new CallResult<TClientReturn, TBackendReturn>();
        const {mode = 'both'} = callOptions;

        const promises = [
            mode === 'both' || mode === 'client' ? this.callClient(args) : null,
            mode === 'both' || mode === 'backend'
                ? this.callBackend(args)
                : null,
        ] as const;

        const [clientResult, backendResult] = await Promise.all(promises);

        result.setClient(clientResult);
        result.setBackend(backendResult);

        return result;
    }
}
