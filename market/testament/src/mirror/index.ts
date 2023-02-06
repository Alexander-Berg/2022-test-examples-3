import type Layer from './layer';
import {resolveScriptPath} from '../utils/relativePath';
import WorkerClient from './worker/client';
import WorkerBackend from './worker/backend';
import Method from './method';

const workerPath = resolveScriptPath(__filename, './worker/process.js');

export {default as PackedFunction, packFunction} from './packedFunction';

export default class Mirror {
    #layers = new Map<
        string,
        {layer: Layer<Record<string, Method<any, any>>, unknown>}
    >();

    #worker = new WorkerClient<WorkerBackend>(workerPath);

    constructor() {
        // @ts-ignore
        global.testamentMirror = this;
    }

    async registerLayer(
        layer: Layer<Record<string, Method<any, any>>, unknown>,
        autoInit = true,
    ): Promise<void> {
        if (layer.backendWorkerPath) {
            const worker = this.#worker;
            await worker.backend.register({
                layer: {
                    id: layer.id,
                    backendPath: layer.backendWorkerPath,
                },
            });
        }

        await this._registerLayer(layer, autoInit);
    }

    async registerRuntime(
        layer: Layer<Record<string, Method<any, any>>, unknown>,
        autoInit = true,
    ): Promise<void> {
        if (layer.backendWorkerPath) {
            await this.#worker.backend.registerRuntime({
                layer: {
                    id: layer.id,
                    backendPath: layer.backendWorkerPath,
                },
            });
        }

        await this._registerLayer(layer, autoInit);
    }

    async _registerLayer(
        layer: Layer<Record<string, Method<any, any>>, unknown>,
        autoInit: boolean,
    ): Promise<void> {
        layer.register(this);
        this.#layers.set(layer.id, {layer});

        if (autoInit) {
            await layer.init();
        }
    }

    getLayer<TLayer extends Layer<Record<string, Method<any, any>>, unknown>>(
        id: string,
    ): TLayer | null {
        const layer = this.#layers.get(id)?.layer;

        if (layer) {
            return layer as TLayer;
        }

        return null;
    }

    call<TResult>(
        layer: Layer<Record<string, Method<any, any>>, unknown>,
        method: string,
        args?: any[],
    ): Promise<TResult> {
        return this.#worker.backend.call<TResult>({
            layer: {
                id: layer.id,
            },
            method,
            args,
        });
    }

    destroy(): void {
        this.#worker
            .end()
            .catch(e =>
                console.log(`Error while destroying worker ${e.message}`),
            );

        Promise.all([
            ...Array.from(this.#layers).map(([, val]) => val.layer.destroy()),
        ]).catch(e =>
            console.log(`Error while destroying mirror ${e.message}`),
        );
    }
}
