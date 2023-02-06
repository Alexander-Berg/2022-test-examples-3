import {PidgetCore} from '~/pidgets/root';
import {makeInitialState} from './state';
import type {State} from './state';
import {reducer} from './reducer';
import {View} from './View';
import * as epics from './epics';

export const TestPidget = PidgetCore.createPidget<State>({
    View,
    name: 'TestPidget',
    makeInitialState,
    epics,
    reducer,
});
