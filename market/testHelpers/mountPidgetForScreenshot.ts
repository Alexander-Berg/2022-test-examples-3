import {mountPidgetCommon, MountComponentArgs} from './mountPidgetCommon';

export function mountPidgetForScreenshot<S, P = any, I = any, T = any>(args: MountComponentArgs<S, P, I, T>) {
    const {pidget} = mountPidgetCommon({...args, isScreenshot: true});

    return {pidget};
}
