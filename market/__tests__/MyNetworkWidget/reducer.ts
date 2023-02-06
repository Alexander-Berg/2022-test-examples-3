import {GenericAction} from '@yandex-market/apiary/common/actions';

import type {Data} from '.';

export default function (data: Data, action: GenericAction): Data {
    switch (action.type) {
        case '#TWO': {
            return {...data, content: (action as any).payload.content};
        }
        default:
            return data;
    }
}
