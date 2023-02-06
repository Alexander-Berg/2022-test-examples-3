import { Action } from 'redux-actions'; // eslint-disable-line import/named

export enum ActionTypes {
    SHOW_TOOLTIP = '[tests] show tooltip',
    HIDE_TOOLTIP = '[tests] hide tooltip'
}

export interface ITestsState {
    visibleLockName: string | null
}

interface IShowTooltipPayload {
    lockName: string
}

export interface IShowTooltipAction extends Action<IShowTooltipPayload> {
    type: ActionTypes.SHOW_TOOLTIP,
    payload: IShowTooltipPayload
}

export interface IHideTooltipAction extends Action<null> {
    type: ActionTypes.HIDE_TOOLTIP
}

export type TestsAction =
    | IShowTooltipAction
    | IHideTooltipAction;

export type TestsPayload =
    | IShowTooltipPayload;
