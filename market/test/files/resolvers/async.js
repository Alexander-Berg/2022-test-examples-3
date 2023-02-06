// eslint-disable-next-line import/extensions,import/no-unresolved
import {createResolver} from '@yandex-market/mandrel/resolver';

export const asyncResolver = createResolver(
    ctx => Promise.resolve({asyncResolver: true, ctx}),
    {name: 'asyncResolver'},
);
