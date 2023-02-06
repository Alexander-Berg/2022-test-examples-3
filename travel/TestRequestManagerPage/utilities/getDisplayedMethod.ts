import {Method} from 'axios';

export function getDisplayedMethod(method: Method): string {
    return method.toUpperCase();
}
