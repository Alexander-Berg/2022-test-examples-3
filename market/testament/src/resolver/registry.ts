export type PackageJSON = {
    name: string;
    version: string;
    main: string;
    browser: string;
};
export type Handler<TArg, TReturn = TArg> = (pkg: TArg) => TReturn;
export type PackageJSONHandlerDescriptor = {
    type: 'packageJSON';
    data: {
        name: string;
    };
    handler: Handler<PackageJSON>;
};
export type HandlerDescriptor = PackageJSONHandlerDescriptor;

const descriptors: Set<HandlerDescriptor> = new Set();

export function register(descriptor: HandlerDescriptor): void {
    descriptors.add(descriptor);
}

export function iterator(): IterableIterator<HandlerDescriptor> {
    return descriptors.values();
}

export function makePackageJSONDescriptor(
    name: string,
    handler: Handler<PackageJSON>,
): PackageJSONHandlerDescriptor {
    return {
        type: 'packageJSON',
        data: {name},
        handler,
    };
}
