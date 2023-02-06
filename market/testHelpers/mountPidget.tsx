import {render} from '@testing-library/react';

import {mountPidgetCommon, MountComponentArgs} from './mountPidgetCommon';

export function mountPidget<S, P = any, I = any, T = any>(args: MountComponentArgs<S, P, I, T>) {
    const {pidget, store} = mountPidgetCommon(args);

    const renderedPidget = render(pidget);

    return {
        pidget: renderedPidget,
        rerender: (props: P) => renderedPidget.rerender(mountPidgetCommon({...args, props}).pidget),
        store,
    };
}
