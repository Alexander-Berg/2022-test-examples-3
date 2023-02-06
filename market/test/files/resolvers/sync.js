// eslint-disable-next-line import/extensions,import/no-unresolved
import {createSyncResolver} from '@yandex-market/mandrel/resolver';

export const syncResolver = createSyncResolver(
    ctx => ({syncResolver: true, ctx}),
    {name: 'syncResolver'},
);
