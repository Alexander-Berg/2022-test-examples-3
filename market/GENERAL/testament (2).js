// @flow

import {createSyncResolver} from '@yandex-market/mandrel/resolver';
import type {Context} from '@yandex-market/mandrel/context';

/**
 * Перед тем как использовать, нужно проконсультироваться в Саппорте Тестамента
 */
export const resolveIsTestamentRuntimeSync = createSyncResolver(
    (ctx: Context): boolean => Boolean(process.env.TESTAMENT_RENDER_PROCESS),
    {name: 'resolveIsTestamentRuntimeSync'}
);
