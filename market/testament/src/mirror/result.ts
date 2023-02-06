export default class CallResult<TClientResult, TBackendResult = TClientResult> {
    #clientResult: TClientResult | null = null;

    #backendResult: TBackendResult | null = null;

    getClient(): TClientResult | null {
        return this.#clientResult;
    }

    setClient(data: TClientResult | null): void {
        this.#clientResult = data;
    }

    getBackend(): TBackendResult | null {
        return this.#backendResult;
    }

    setBackend(data: TBackendResult | null): void {
        this.#backendResult = data;
    }
}
