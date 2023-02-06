import React, { createContext, useCallback, useEffect, useMemo, useState } from 'react';

import { CheckoutMessage, CheckoutMessageType, CheckoutWebSocket } from '../../../lib/websocket';
import { MessageType } from '../../../types/message';
import { useUrlParams } from '../../../hooks/useUrlParams';

type PageContextType = {
    pageId?: string;
    setPageId: (id: string) => void;
    connect: () => void;
    disconnect: () => void;
    isSocketConnected: boolean;
    isMerchantConnected: boolean;
    isStarted: boolean;
    sendMessage: (data: object) => void;
    event?: string;
    checkoutState?: object;
};

export const PageContext = createContext<PageContextType>({
    setPageId: () => {},
    sendMessage: () => {},
    connect: () => {},
    disconnect: () => {},
    isSocketConnected: false,
    isMerchantConnected: false,
    isStarted: false,
});

const PageProvider: React.FC = ({ children }) => {
    const urlParams = useUrlParams();
    const [pageId, setPageId] = useState<string>(urlParams.get('pageId') || '');
    const [event, setEvent] = useState<string>();
    const [checkoutState, setCheckoutState] = useState<object | undefined>();
    const [isSocketConnected, setSocketConnected] = useState(false);
    const [isMerchantConnected, setMerchantConnected] = useState(false);
    const [websocket, setWebsocket] = useState<CheckoutWebSocket>();

    const connect = useCallback(() => {
        setWebsocket(new CheckoutWebSocket(pageId));
    }, [pageId]);

    const disconnect = useCallback(() => {
        setEvent(undefined);
        setCheckoutState(undefined);
        setWebsocket(undefined);
    }, []);

    useEffect(() => {
        if (!websocket) {
            return;
        }

        const onMessage = (message: CheckoutMessage) => {
            // Принимаем события от магазина

            switch (message.type) {
                case CheckoutMessageType.Connected:
                    setMerchantConnected(true);
                    break;

                case CheckoutMessageType.Disconnected:
                    setMerchantConnected(false);
                    break;

                case CheckoutMessageType.Event:
                    setEvent(message.data.event);
                    setCheckoutState(message.data.checkoutState);
                    break;
            }
        };

        const onConnect = () => {
            setSocketConnected(true);
            websocket.send(MessageType.EventManagerInit, {});
        };
        const onDisconnect = () => {
            setSocketConnected(false);
            setMerchantConnected(false);
        };

        websocket.addEventHandler('message', onMessage);
        websocket.addEventHandler('open', onConnect);
        websocket.addEventHandler('close', onDisconnect);

        websocket.connect();

        return () => {
            setSocketConnected(false);
            setMerchantConnected(false);

            websocket.removeEventHandler('message', onMessage);
            websocket.removeEventHandler('open', onConnect);
            websocket.removeEventHandler('close', onDisconnect);

            websocket.close();
        };
    }, [websocket]);

    const sendMessage = useCallback(
        (data: object) => {
            // Отвечая магазину, надо сброситть событие
            setEvent(undefined);
            setCheckoutState(undefined);

            websocket?.send(MessageType.EventManagerMessage, data);
        },
        [websocket]
    );

    const contextValue = useMemo<PageContextType>(
        () => ({
            sendMessage,
            pageId,
            setPageId,
            event,
            checkoutState,
            connect,
            disconnect,
            isSocketConnected,
            isMerchantConnected,
            isStarted: Boolean(websocket),
        }),
        [
            sendMessage,
            pageId,
            event,
            checkoutState,
            isSocketConnected,
            isMerchantConnected,
            websocket,
            connect,
            disconnect,
        ]
    );

    return <PageContext.Provider value={contextValue}>{children}</PageContext.Provider>;
};

export default PageProvider;
