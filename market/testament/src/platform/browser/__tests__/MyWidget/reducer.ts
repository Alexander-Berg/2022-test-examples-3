import {GenericAction} from '@yandex-market/apiary/common/actions';

import type {Data} from '.';

export default function (data: Data, action: GenericAction): Data {
    switch (action.type) {
        case '#ONE': {
            return {...data, state: 'loading'};
        }
        case '#TWO': {
            return {...data, items: [], state: 'done'};
        }
        case '#INC': {
            // @ts-ignore
            return {...data, touches: data.touches + action.payload.amount};
        }
        default:
            return data;
    }
}
