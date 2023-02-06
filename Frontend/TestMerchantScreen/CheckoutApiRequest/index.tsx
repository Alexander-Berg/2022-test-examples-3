import React, { useCallback, useContext, useState } from 'react';
import {
    YandexCheckoutRequestUpdateEvent,
    YandexCheckoutDetails,
    YandexCheckoutEvent,
} from '@yandex-int/tap-checkout-types';

import { CheckoutState as CheckoutStateType } from '../../../types/checkout-api';

import { Button } from '../../../components/Button';
import { Text } from '../../../components/Text';
import CheckoutState from '../../../components/CheckoutState';

import { PageContext } from '../PageProvider';
import { ConnectionStatus } from '../ConnectionStatus';

import styles from './CheckoutApiRequest.module.css';

const eventTypes: YandexCheckoutEvent[] = [
    'restoreState',
    'cityChange',
    'shippingOptionChange',
    'shippingAddressChange',
    'pickupOptionChange',
    'datetimeOptionChange',
    'promoCodeChange',
    'paymentOptionChange',
    'paymentStart',
    'paymentError',
];

const buildError = (error: unknown): string => {
    if (error instanceof Error) {
        return error.toString();
    }

    if (typeof error === 'string') {
        return error;
    }

    return JSON.stringify(error);
};

const CheckoutApiRequest: React.FC = () => {
    const [isStarted, setStarted] = useState(false);
    const [isLoading, setLoading] = useState(false);
    const [result, setResult] = useState<CheckoutStateType | null>(null);
    const [error, setError] = useState<string>('');
    const { shopName, shopIcon, baseUrl, serviceToken, sendMessage } = useContext(PageContext);

    const onEventTriggered = useCallback(
        (event: YandexCheckoutRequestUpdateEvent) => {
            const { type, checkoutState } = event;

            // eslint-disable-next-line no-console
            console.log('[DEBUG] Native Event', { type, checkoutState });

            event.updateWith(
                sendMessage({
                    event: type,
                    checkoutState,
                })
            );
        },
        [sendMessage]
    );

    const onCheckoutOpen = useCallback(() => {
        setStarted(true);
        setLoading(true);
        setResult(null);
        setError('');

        sendMessage({ event: 'getCheckoutDetails', checkoutState: null })
            .then(checkoutDetails => {
                setLoading(false);

                if (!checkoutDetails) {
                    throw new Error('initial CheckoutDetails is null');
                }

                const checkoutRequest = new window.YandexCheckoutRequest(checkoutDetails as YandexCheckoutDetails, {
                    shopName,
                    shopIcon,
                    baseUrl,
                    serviceToken,
                });

                eventTypes.forEach(eventType => checkoutRequest.addEventListener(eventType, onEventTriggered));

                const cleanup = () => {
                    eventTypes.forEach(eventType => {
                        checkoutRequest.removeEventListener(eventType, onEventTriggered);
                    });
                };

                return checkoutRequest.show().finally(() => cleanup());
            })
            .then(checkoutState => {
                setResult(checkoutState);
                setStarted(false);
            })
            .catch(message => {
                setError(buildError(message));
                setStarted(false);
            });
    }, [onEventTriggered, sendMessage, shopName, shopIcon, baseUrl, serviceToken]);

    return (
        <>
            <Button disabled={isLoading} view="default" size="m" onClick={onCheckoutOpen}>
                {isLoading ? 'Ожидает получения стартового CheckoutDetails...' : 'Открыть чекаут'}
            </Button>

            {(result || error) && (
                <div>
                    <Text typography="headline-xs" weight="light" as="div">
                        Результат:
                    </Text>
                    {result && (
                        <>
                            <CheckoutState checkoutState={result} />
                        </>
                    )}
                    {error && (
                        <Text typography="headline-xs" weight="light" className={styles.error}>
                            {error}
                        </Text>
                    )}
                </div>
            )}

            {isStarted && <ConnectionStatus />}
        </>
    );
};

export default CheckoutApiRequest;
