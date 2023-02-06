/* eslint-disable no-shadow, global-require */

import {RuntimeBackend} from '../layers/runtime';

export type BackendDescriptor<TAPI> = {
    backend: TAPI;
};

export type CallMethodArg<TArgs extends readonly any[]> = {
    layer: {id: string};
    method: string;
    args?: TArgs;
};

export type RegisterMethodArg = {
    layer: {
        id: string;
        backendPath: string;
    };
};

export default class WorkerBackend {
    registry = new Map<string, BackendDescriptor<any>>();

    runtimeLayer = '';

    constructor() {
        process.env.TESTAMENT_RENDER_PROCESS = '1';
        // @ts-ignore
        global.getBackend = this.getBackend.bind(this);
    }

    getBackend<TAPI>(id: string): TAPI | null {
        const descriptor = this.registry.get(id);
        return descriptor?.backend ?? null;
    }

    getRuntimeBackend(): RuntimeBackend | null {
        return this.getBackend<RuntimeBackend>(this.runtimeLayer);
    }

    async call<TResult>(arg: CallMethodArg<unknown[]>): Promise<TResult> {
        // eslint-disable-next-line no-undef
        const runtimeBackend = this.getBackend<RuntimeBackend>(
            this.runtimeLayer,
        );
        const withRuntime =
            runtimeBackend && arg.layer.id !== this.runtimeLayer;
        const backendName = arg.layer.id;
        const methodName = arg.method;
        const methodArgs = arg.args || [];
        let result;

        if (withRuntime) {
            result = await runtimeBackend?.runCode(
                ((
                    backendName: string,
                    methodName: string,
                    methodArgs: any[],
                ) => {
                    // @ts-ignore
                    // eslint-disable-next-line no-undef
                    const backend = getBackend(backendName);
                    // @ts-ignore
                    return backend[methodName].call(backend, ...methodArgs);
                }).toString(),
                [backendName, methodName, methodArgs] as const,
            );
        } else {
            // eslint-disable-next-line no-undef
            const backend = this.getBackend(backendName);
            // @ts-ignore
            result = await backend[methodName].call(backend, ...methodArgs);
        }

        return result;
    }

    registerRuntime(arg: RegisterMethodArg): void {
        this.register(arg);
        this.runtimeLayer = arg.layer.id;
    }

    register(arg: RegisterMethodArg): void {
        // eslint-disable-next-line no-undef
        const runtimeBackend = this.getBackend<RuntimeBackend>(
            this.runtimeLayer,
        );
        let backendInstance = null;

        if (arg.layer.backendPath) {
            // todo сделать дефолтный нодовый рантайм чтобы избавить от условия
            // todo добавить в воркер методы requireModule, runCode проксирующие в рантайм
            if (runtimeBackend) {
                backendInstance = runtimeBackend.requireModule(
                    arg.layer.backendPath,
                );
            } else {
                backendInstance = require(arg.layer.backendPath);
            }
        }

        if (backendInstance?.default) {
            backendInstance = backendInstance?.default;
        }

        this.registry.set(arg.layer.id, {backend: backendInstance});
    }
}
