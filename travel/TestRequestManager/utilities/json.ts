import every from 'lodash/every';
import isPlainObject from 'lodash/isPlainObject';

import {TJsonEntity} from 'server/utilities/TestRequestManager/types/json';

export function isJson(value: unknown): value is TJsonEntity {
    if (
        typeof value === 'number' ||
        typeof value === 'string' ||
        typeof value === 'boolean' ||
        value === null
    ) {
        return true;
    }

    if (
        Array.isArray(value) ||
        (typeof value === 'object' && isPlainObject(value))
    ) {
        return every(value, isJson);
    }

    return false;
}
