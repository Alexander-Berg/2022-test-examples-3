import produce from 'immer';

export function producer<T, C, F = T>(
    composer: (items: F[]) => C,
    creatorFactory = entityCreatorFactory,
) {
    return function producerFactory(factory: (partialItem?: Partial<T>) => F) {
        const creator = creatorFactory(factory);

        function compose(n: number): C;
        function compose(...args: Partial<T>[]): C;
        function compose(...args: any[]) {
            return composer(creator(...args));
        }

        return compose;
    };
}

export function entityCreatorFactory<T, F>(factory: (partialItem: T | undefined, index: number) => F) {
    function creator(): F[];
    function creator(n: number): F[];
    function creator(...args: Partial<T>[]): F[];
    function creator(...args: any[]) {
        if (!args.length) {
            try {
                return [factory(undefined, 0)];
            } catch {
                return [];
            }
        }

        if (args.length === 1 && typeof args[0] === 'number') {
            const items: F[] = [];

            for (let i = 0; i < args[0]; i++) {
                items.push(factory(undefined, i));
            }

            return items;
        }

        return args.map(factory);
    }

    return creator;
}

export function objectComposer<T, C>(getId: (entity: T) => string | number, base?: Partial<C>) {
    return (items: T[]) => {
        return items.reduce((aux, item) => {
            aux[getId(item)] = item;

            return aux;
        }, (base || {}) as C);
    };
}

export function mutate<O>(object: O, partial: Partial<O>) {
    return produce(object, (draft) => {
        Object.assign(draft, partial);
    });
}

export function getRandomCount<T>(source: T[], count: number) {
    const resorted = [...source].sort(() => Math.random() - Math.random());

    return resorted.slice(0, count);
}
