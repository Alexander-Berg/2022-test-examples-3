import {ActionsObservable} from 'redux-observable';
import {Observable} from 'rxjs';
import {WidgetEpic, WidgetFacade} from '@yandex-market/apiary';
import {GenericAction} from '@yandex-market/apiary/common/actions';

export type Collections = {
    [key: string]: {
        [key: string]: unknown;
    };
};

export type CallEpicFn<TAction extends GenericAction> = (
    action: TAction,
) => Observable<GenericAction>;

export type PrepareEpicFn<
    TAction extends GenericAction,
    TWidgetData,
    TCollections extends Collections,
> = (
    epic: WidgetEpic<TAction, TWidgetData, TCollections>,
) => CallEpicFn<TAction>;

export default function create<TWidgetData, TCollections extends Collections>(
    widgetData: TWidgetData,
    collections: TCollections,
): PrepareEpicFn<GenericAction, TWidgetData, TCollections> {
    const facade: WidgetFacade<TWidgetData, TCollections> = {
        getData: () => widgetData,
        getCollections: () => collections,
    };

    return function prepare<TAction extends GenericAction>(
        epic: WidgetEpic<TAction, TWidgetData, TCollections>,
    ): CallEpicFn<TAction> {
        // eslint-disable-next-line no-shadow
        return function callEpic(action: TAction): Observable<GenericAction> {
            return epic(ActionsObservable.of(action), facade);
        };
    };
}

export function callEpic<
    TWidgetData,
    TAction extends GenericAction,
    TCollections extends Collections,
>(
    state: GenericAction,
    epic: WidgetEpic<TAction, TWidgetData, TCollections>,
    action: TAction,
): Observable<GenericAction> {
    // @ts-ignore
    return create(state.widgetData, state.collections)(epic)(action);
}
