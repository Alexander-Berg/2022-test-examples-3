import { handleActions } from 'redux-actions';

import {
    ActionTypes,
    TestsPayload,
    ITestsState, IShowTooltipAction
} from './types';

const initialState: ITestsState = {
    visibleLockName: null
};

function showTooltip(
    state: ITestsState,
    action: IShowTooltipAction
): ITestsState {
    const { lockName } = action.payload;

    return Object.assign({}, state, { visibleLockName: lockName });
}

function hideTooltip(
    state: ITestsState
) : ITestsState {
    return Object.assign({}, state, { visibleLockName: null });
}

export default handleActions<ITestsState, TestsPayload>({
    [ActionTypes.SHOW_TOOLTIP]: showTooltip,
    [ActionTypes.HIDE_TOOLTIP]: hideTooltip
}, initialState);
