import Method, {ExtractMethods} from './method';

import type Mirror from '.';

export interface LayerWithInit {
    init(): Promise<any>;
}

export default abstract class Layer<
    TMethods extends Record<string, Method<any, any>>,
    TWorker,
> implements LayerWithInit
{
    #mirror: Mirror | null = null;

    readonly id: string;

    readonly backendWorkerPath: string | undefined;

    readonly methods: Readonly<TMethods>;

    readonly client: ExtractMethods<TMethods, 'client'>;

    readonly backend: ExtractMethods<TMethods, 'backend'>;

    readonly worker: TWorker;

    protected constructor(id: string, backendWorkerPath?: string) {
        this.id = id;
        this.backendWorkerPath = backendWorkerPath;
        this.methods = this.getMethods();
        // @ts-ignore
        this.backend = new Proxy({} as ExtractMethods<TMethods, 'backend'>, {
            get:
                (_, method: string): ((...args: any[]) => Promise<any>) =>
                (...args: any[]) =>
                    this.methods[method].callBackend(args),
        });
        this.client = new Proxy({} as ExtractMethods<TMethods, 'client'>, {
            get:
                (_, method: string): ((...args: any[]) => Promise<any>) =>
                (...args: any[]) =>
                    this.methods[method].callClient(args),
        });
        // @ts-ignore
        this.worker = new Proxy({} as TWorker, {
            get:
                (_, method: string): ((...args: any[]) => Promise<any>) =>
                (...args: any[]) =>
                    this.call(method, args),
        });
    }

    abstract getMethods(): TMethods;

    register(mirror: Mirror): void {
        this.#mirror = this.#mirror ?? mirror;
    }

    getMirror(): Mirror | null {
        return this.#mirror;
    }

    async call<TResult>(method: string, args?: any[]): Promise<TResult | null> {
        return this.#mirror?.call<TResult>(this, method, args) ?? null;
    }

    // eslint-disable-next-line class-methods-use-this
    init(): Promise<void> {
        return Promise.resolve();
    }

    // eslint-disable-next-line class-methods-use-this
    destroy(): Promise<void> {
        return Promise.resolve();
    }
}
