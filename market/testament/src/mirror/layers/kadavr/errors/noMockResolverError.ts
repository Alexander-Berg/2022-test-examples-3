export class NoMockResolverError extends Error {
    constructor(host: string) {
        super(`No mock resolved for host ${host}`);

        this.name = NoMockResolverError.name;
    }
}
