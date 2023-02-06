export class NoMockRouteError extends Error {
    constructor(host: string, pathname: string) {
        super(
            `No route in mock resolved for host ${host} and pathname ${pathname}`,
        );
        this.name = NoMockRouteError.name;
    }
}
