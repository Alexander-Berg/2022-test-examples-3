import {Promisable} from '@yandex-market/apiary/util/promise';
import {
    Context,
    getStoutUser,
    // @ts-ignore
} from '@yandex-market/mandrel/context';

import {allItems} from './data';

import type {Collections, Data, Options} from '.';

export default function (ctx: Context, {items}: Options): Promisable<any> {
    return {
        data: Promise.resolve({
            items,
            state: 'none',
            isRobot: getStoutUser(ctx).isRobot(),
            touches: 0,
        }) as Promise<Data>,
        collections: {
            list: allItems,
        } as Collections,
    };
}
