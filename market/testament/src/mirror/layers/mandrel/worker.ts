import type {Context} from '@yandex-market/mandrel/context';

import {createContext, InitContextArg} from './contextHelpers';

export class MandrelWorker {
    #context: Context | null = null;

    getContext(): Context | null {
        return this.#context;
    }

    initContext(params?: InitContextArg): void {
        this.#context = createContext(params);
    }
}

export default new MandrelWorker();
