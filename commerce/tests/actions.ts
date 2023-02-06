import {
    ActionTypes,
    IShowTooltipAction,
    IHideTooltipAction
} from './types';

export function showTooltip(lockName: string): IShowTooltipAction {
    return { type: ActionTypes.SHOW_TOOLTIP, payload: { lockName } };
}

export function hideTooltip(): IHideTooltipAction {
    return { type: ActionTypes.HIDE_TOOLTIP };
}
