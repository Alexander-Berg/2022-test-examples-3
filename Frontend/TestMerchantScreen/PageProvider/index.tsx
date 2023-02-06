import React, { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { YandexCheckoutDetailsUpdate } from '@yandex-int/tap-checkout-types';

import { MessageType } from '../../../types/message';
import { CheckoutMessage, CheckoutMessageType, CheckoutWebSocket } from '../../../lib/websocket';
import { useUrlParams } from '../../../hooks/useUrlParams';

type PageContextType = {
    pageId: string;
    setPageId: (id: string) => void;
    shopName: string | undefined;
    setShopName: (name: string | undefined) => void;
    shopIcon: string | undefined;
    setShopIcon: (url: string | undefined) => void;
    baseUrl: string | undefined;
    setBaseUrl: (url: string | undefined) => void;
    serviceToken: string | undefined;
    setServiceToken: (url: string | undefined) => void;
    isSocketConnected: boolean;
    isAdminConnected: boolean;
    sendMessage: (data: object) => Promise<YandexCheckoutDetailsUpdate | null>;
};

export const PageContext = createContext<PageContextType>({
    pageId: '',
    setPageId: () => {},
    shopName: '',
    setShopName: () => {},
    shopIcon: '',
    setShopIcon: () => {},
    baseUrl: '',
    setBaseUrl: () => {},
    serviceToken: '',
    setServiceToken: () => {},
    isSocketConnected: false,
    isAdminConnected: false,
    sendMessage: () => Promise.resolve(null),
});

const defaultShop = {
    name: 'Беру',
    icon: 'https://yastatic.net/market-export/_/i/favicon/pokupki/196.png',
    baseUrl: '/turbo',
    serviceToken: '',
};

const PageProvider: React.FC = ({ children }) => {
    const urlParams = useUrlParams();
    const [pageId, setPageId] = useState(urlParams.get('pageId') || uuidv4());
    const [websocket, setWebsocket] = useState<CheckoutWebSocket>();
    const [isSocketConnected, setSocketConnected] = useState(false);
    const [isAdminConnected, setAdminConnected] = useState(false);
    const [queue, setQueue] = useState<Array<() => Promise<unknown>>>([]);
    const [shopName, setShopName] = useState<string | undefined>(defaultShop.name);
    const [shopIcon, setShopIcon] = useState<string | undefined>(defaultShop.icon);
    const [baseUrl, setBaseUrl] = useState<string | undefined>(defaultShop.baseUrl);
    const [serviceToken, setServiceToken] = useState<string | undefined>(defaultShop.serviceToken);

    useEffect(() => {
        if (!pageId) {
            setWebsocket(undefined);
            return;
        }

        const socket = new CheckoutWebSocket(pageId);

        const onConnect = () => {
            socket.send(MessageType.MerchantInit, {});
            setSocketConnected(true);
        };
        const onDisconnect = () => {
            setSocketConnected(false);
            setAdminConnected(false);
        };
        const onMessage = (e: CheckoutMessage) => {
            switch (e.type) {
                case CheckoutMessageType.Connected:
                    setAdminConnected(true);
                    break;

                case CheckoutMessageType.Disconnected:
                    setAdminConnected(false);
                    break;
            }
        };

        socket.addEventHandler('open', onConnect);
        socket.addEventHandler('close', onDisconnect);
        socket.addEventHandler('message', onMessage);

        socket.connect();
        setWebsocket(socket);

        return () => {
            setSocketConnected(false);
            setAdminConnected(false);

            socket.removeEventHandler('open', onConnect);
            socket.removeEventHandler('close', onDisconnect);
            socket.removeEventHandler('message', onMessage);

            socket.close();
        };
    }, [pageId]);

    const sendMessage = useCallback(
        (data: object) => {
            return new Promise<YandexCheckoutDetailsUpdate | null>(resolve => {
                const fn = () => {
                    return new Promise(resolveQueue => {
                        if (!websocket) {
                            return resolve(null);
                        }

                        websocket.addEventHandler('message', function handler(message: CheckoutMessage) {
                            if (message.type !== CheckoutMessageType.Event) {
                                return;
                            }

                            // eslint-disable-next-line no-console
                            console.log('[DEBUG] WebSocket Event', message);

                            resolve((message.data.checkoutDetails as YandexCheckoutDetailsUpdate) ?? null);
                            resolveQueue();

                            websocket.removeEventHandler('message', handler);
                        });

                        websocket.send(MessageType.MerchantMessage, data);
                    });
                };

                setQueue(queue => queue.concat(fn));
            });
        },
        [websocket]
    );

    useEffect(() => {
        if (!isSocketConnected || !isAdminConnected || queue.length === 0) {
            return;
        }

        queue[0]().then(() => {
            setQueue(queue => queue.slice(1));
        });
    }, [queue, isSocketConnected, isAdminConnected]);

    const contextValue = useMemo(
        () => ({
            pageId,
            setPageId,
            shopName,
            setShopName,
            shopIcon,
            setShopIcon,
            baseUrl,
            setBaseUrl,
            serviceToken,
            setServiceToken,
            isSocketConnected,
            isAdminConnected,
            sendMessage,
        }),
        [pageId, shopName, shopIcon, baseUrl, serviceToken, isSocketConnected, isAdminConnected, sendMessage]
    );

    return <PageContext.Provider value={contextValue}>{children}</PageContext.Provider>;
};

export default PageProvider;
