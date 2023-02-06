import {ActionType, createReducer} from 'typesafe-actions';

import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';

import * as actions from 'reducers/testControlPanel/actions';

export interface ITestControlPanelState {
    apiRequestInfoItems: IApiRequestInfo[];
}

const initialState: ITestControlPanelState = {
    apiRequestInfoItems: [],
};

export default createReducer<
    ITestControlPanelState,
    ActionType<typeof actions>
>(initialState).handleAction(
    actions.setApiRequestInfoItems,
    (state, {payload}) => ({
        ...state,
        apiRequestInfoItems: payload,
    }),
);
