import {MINUTE, SECOND} from 'utilities/dateUtils/constants';

export function getHumanDuration(ms: number): string {
    if (ms < SECOND) {
        return `${ms} мс`;
    }

    if (ms < MINUTE) {
        return `${(ms / SECOND).toFixed(1)} с`;
    }

    return `${(ms / MINUTE).toFixed(1)} мин`;
}
