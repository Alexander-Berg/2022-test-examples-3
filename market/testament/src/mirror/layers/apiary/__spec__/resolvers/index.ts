import {createResolver} from '@yandex-market/mandrel/resolver';

export default createResolver((ctx, params: {foo: number}) =>
    Promise.resolve(params.foo),
);
