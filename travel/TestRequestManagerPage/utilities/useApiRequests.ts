import {useEffect, useState} from 'react';
import {useSelector} from 'react-redux';

import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';
import {TRequestEvent} from 'server/utilities/TestRequestManager/types/events';

import {getApiRequestInfoItems} from 'projects/testControlPanel/pages/TestRequestManagerPage/selectors/apiRequests';

import useImmutableCallback from 'utilities/hooks/useImmutableCallback';
import pageManager from 'utilities/pageManager/pageManager';
import {handleRequestEvent} from 'server/utilities/TestRequestManager/utilities/handleRequestEvent';

export interface IUseApiRequestsOptions {
    filterByPage: boolean;
}

interface IUseApiRequestsReturnValue {
    apiRequests: IApiRequestInfo[];

    clearRequests(): void;
}

export function useApiRequests(
    options: IUseApiRequestsOptions,
): IUseApiRequestsReturnValue {
    const {filterByPage} = options;

    const initialApiRequests = useSelector(getApiRequestInfoItems);
    const [apiRequests, setApiRequests] = useState<IApiRequestInfo[]>(
        filterByPage ? initialApiRequests : [],
    );

    useEffect(() => {
        let socket: WebSocket;

        if (filterByPage && pageManager.apiRequestsSocket) {
            socket = pageManager.apiRequestsSocket;
        }

        socket ??= pageManager.initApiRequestsSocket(filterByPage).socket;

        socket.addEventListener('message', event => {
            if (event.data !== 'ping') {
                const requestEvent: TRequestEvent = JSON.parse(event.data);

                setApiRequests(requests =>
                    handleRequestEvent(requests, requestEvent, true),
                );
            }
        });

        return () => {
            socket.close();
        };
    }, [filterByPage]);

    return {
        apiRequests,
        clearRequests: useImmutableCallback(() => {
            setApiRequests([]);
        }),
    };
}
