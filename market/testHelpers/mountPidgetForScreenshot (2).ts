import {mountPidgetCommon, MountComponentArgs} from './mountPidgetCommon';

export function mountPidgetForScreenshot<S, P = any, I = any>(args: MountComponentArgs<S, P, I>) {
    const {pidget} = mountPidgetCommon({...args, isScreenshot: true});

    return {pidget};
}
