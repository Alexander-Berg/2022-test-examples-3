import {mountPidgetCommon, MountComponentArgs} from './mountPidgetCommon';

export function mountPidget<S, P = any, I = any>(args: MountComponentArgs<S, P, I>) {
    const {pidget, pidgetForCat, store} = mountPidgetCommon(args);

    return {pidget, pidgetForCat, store};
}
