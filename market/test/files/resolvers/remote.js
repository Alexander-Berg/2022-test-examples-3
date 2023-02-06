// eslint-disable-next-line import/extensions,import/no-unresolved
import {createResolver} from '@yandex-market/mandrel/resolver';

export const remoteResolver = createResolver(
    ctx => Promise.resolve({remoteResolver: true, ctx}),
    {name: 'remoteResolver', remote: true},
);
