import { MutableRefObject, useCallback, useContext, useEffect } from 'react';

import { CheckoutState } from '../../../types/checkout-api';

import { PageContext } from '../PageProvider';

type Args = {
    frameSrc?: string;
    iframeRef: MutableRefObject<HTMLIFrameElement | null>;
    onSuccess: (state: CheckoutState) => void;
    onError: (message: string) => void;
    onHeightChange: (height: number) => void;
};

const CONTENT_HEIGHT_ANIMATION_TIME = 300;

// Пока не готов checkoutRequestApi показываем форму чекаута во фрейме и
// осуществляем взаимодействие с помощью postMessage
export default ({ frameSrc, iframeRef, onSuccess, onError, onHeightChange }: Args) => {
    const { shopName, shopIcon, baseUrl, serviceToken, sendMessage } = useContext(PageContext);

    const onPostMessage = useCallback(
        (e: MessageEvent) => {
            if (!frameSrc?.includes(e.origin)) {
                return;
            }

            // eslint-disable-next-line no-console
            console.log('[DEBUG] PostMessage Event', e.data);

            const { event, checkoutState } = e.data;
            const checkoutFrame = iframeRef?.current?.contentWindow;

            if (!event) {
                return;
            }

            if (event === 'setContentHeight') {
                onHeightChange(e.data.height);
                setTimeout(
                    () => checkoutFrame?.postMessage({ event }, '*'),
                    CONTENT_HEIGHT_ANIMATION_TIME
                );
                return;
            }

            if (event === 'closeWithSuccess') {
                onSuccess(checkoutState);
                return;
            }

            if (event === 'closeWithError') {
                onError(`${e.data.name}: ${e.data.message}`);
                return;
            }

            if (event === 'getMerchantInfo') {
                checkoutFrame?.postMessage(
                    {
                        event,
                        data: {
                            pageUrl: window.location.toString(),
                        },
                    },
                    '*'
                );
                return;
            }

            if (event === 'getCheckoutOptions') {
                checkoutFrame?.postMessage(
                    {
                        event,
                        data: {
                            shopName,
                            shopIcon,
                            baseUrl,
                            serviceToken,
                        },
                    },
                    '*'
                );
                return;
            }

            sendMessage({ event, checkoutState }).then(checkoutDetails => {
                checkoutFrame?.postMessage({ event, data: checkoutDetails }, '*');
            });
        },
        [
            frameSrc,
            sendMessage,
            iframeRef,
            onSuccess,
            onError,
            onHeightChange,
            shopName,
            shopIcon,
            baseUrl,
            serviceToken,
        ]
    );

    useEffect(() => {
        window.addEventListener('message', onPostMessage);

        return () => window.removeEventListener('message', onPostMessage);
    }, [onPostMessage]);
};
