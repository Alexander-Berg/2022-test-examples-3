import {Promisable} from '@yandex-market/apiary/util/promise';

import type {Data} from '.';

export default function (): Promisable<any> {
    return {
        data: Promise.resolve({
            content: 'initial',
        }) as Promise<Data>,
    };
}
