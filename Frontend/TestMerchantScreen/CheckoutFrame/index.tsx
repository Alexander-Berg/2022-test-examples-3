import React, { useState, useCallback, useRef, useEffect } from 'react';
import { useHistory, useLocation } from 'react-router-dom';

import { CheckoutState as CheckoutStateType } from '../../../types/checkout-api';

import { useUrlParams } from '../../../hooks/useUrlParams';

import { Text } from '../../../components/Text';
import { Spin } from '../../../components/Spin';
import { Button } from '../../../components/Button';
import { InputLabel } from '../../../components/InputLabel';
import { Textinput } from '../../../components/TextInput';
import CheckoutState from '../../../components/CheckoutState';

import { ConnectionStatus } from '../ConnectionStatus';

import useCheckoutPostMessageEvent from './useCheckoutPostMessageEvent';
import styles from './CheckoutFrame.module.css';

const initialHeight = 200;

const CheckoutFrame: React.FC = () => {
    const history = useHistory();
    const location = useLocation();

    const iframeRef = useRef<HTMLIFrameElement>(null);
    const urlParams = useUrlParams();
    const [frameSrc, setFrameSrc] = useState<string>();
    const [isFrameLoading, setFrameLoading] = useState(false);
    const [inputValue, setInputValue] = useState(urlParams.get('checkoutUrl') || 'https://checkout.tap-tst.yandex.ru');
    const [height, setHeight] = useState(initialHeight);
    const [result, setResult] = useState<CheckoutStateType | null>(null);
    const [error, setError] = useState('');
    const selfUrl = `${location.pathname}${location.search}${location.hash}`;

    const onInputChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            isFrameLoading && setFrameLoading(false);

            setInputValue(e.target.value);
        },
        [isFrameLoading]
    );

    const onCheckoutOpen = useCallback(() => {
        const url = new URL(inputValue);

        // Хак для iframe. Добавляем GET параметр для сброса sessionStorage и начала новой сессии
        url.searchParams.set('reset_session_storage', 'true');

        setFrameSrc(url.toString());
        setFrameLoading(true);
        setResult(null);
        setError('');
    }, [inputValue]);

    const onFrameLoaded = useCallback(() => setFrameLoading(false), []);

    const onFrameClose = useCallback(() => {
        setFrameSrc(undefined);
        setHeight(initialHeight);
    }, []);

    const onSuccess = useCallback(
        (state: CheckoutStateType) => {
            setResult(state);
            onFrameClose();
        },
        [onFrameClose]
    );

    const onError = useCallback(
        (message: string) => {
            onFrameClose();
            setError(message);
        },
        [onFrameClose]
    );

    useEffect(() => {
        // При открытии iframe добавляем запись в историю из-за бага в Safari TAP-2679
        if (frameSrc && !isFrameLoading) {
            history.push(selfUrl);
        }
    }, [history, selfUrl, frameSrc, isFrameLoading]);

    useCheckoutPostMessageEvent({
        frameSrc,
        iframeRef,
        onSuccess,
        onError,
        onHeightChange: setHeight,
    });

    return (
        <>
            <InputLabel title="Адрес чекаута">
                <Textinput
                    view="default"
                    size="m"
                    type="url"
                    autoComplete="on"
                    value={inputValue}
                    placeholder="Адрес чекаута"
                    onChange={onInputChange}
                    disabled={Boolean(frameSrc)}
                />
            </InputLabel>

            <Button view="default" size="m" onClick={onCheckoutOpen}>
                Открыть чекаут
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

            {frameSrc && (
                <>
                    <ConnectionStatus />
                    <div className={styles.fadeOut}>
                        <div className={styles.modal} style={{ height: height }}>
                            {isFrameLoading && <Spin size="m" view="default" progress className={styles.loader} />}
                            <iframe
                                ref={iframeRef}
                                className={styles.frame}
                                src={frameSrc}
                                onLoad={onFrameLoaded}
                                onError={onFrameLoaded}
                                allow="geolocation"
                            />
                        </div>
                    </div>
                </>
            )}
        </>
    );
};

export default CheckoutFrame;
