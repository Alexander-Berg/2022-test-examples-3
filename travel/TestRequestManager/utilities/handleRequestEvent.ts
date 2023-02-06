import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';
import {
    ERequestEventType,
    TRequestEvent,
} from 'server/utilities/TestRequestManager/types/events';

export function handleRequestEvent(
    apiRequests: IApiRequestInfo[],
    event: TRequestEvent,
    withLocalTimestamp?: boolean,
): IApiRequestInfo[] {
    switch (event.type) {
        case ERequestEventType.START_REQUEST: {
            return [
                ...apiRequests,
                {
                    id: event.id,
                    request: event.request,
                    response: null,
                    isAborted: false,
                    apiHostType: event.apiHostType,
                    source: event.source,
                    startTime: event.timestamp,
                    endTime: null,
                    ...(withLocalTimestamp ? {localStartTime: Date.now()} : {}),
                },
            ];
        }

        case ERequestEventType.END_REQUEST: {
            const existingRequestIndex = apiRequests.findIndex(
                ({id}) => event.id === id,
            );

            if (existingRequestIndex === -1) {
                return apiRequests;
            }

            return [
                ...apiRequests.slice(0, existingRequestIndex),
                {
                    ...apiRequests[existingRequestIndex],
                    response: event.response,
                    isAborted: event.isAborted,
                    endTime: event.timestamp,
                },
                ...apiRequests.slice(existingRequestIndex + 1),
            ];
        }
    }

    return apiRequests;
}
