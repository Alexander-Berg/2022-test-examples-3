import React, { useCallback, useContext, useState } from 'react';

import { CheckoutDetailsUpdate } from '../../../types/checkout-api';

import { Button } from '../../../components/Button';
import { Text } from '../../../components/Text';
import { Textinput } from '../../../components/TextInput';
import { InputLabel } from '../../../components/InputLabel';
import CheckoutDetailsForm from '../../../components/CheckoutDetailsForm';
import CheckoutState from '../../../components/CheckoutState';

import { PageContext } from '../PageProvider';

import styles from './EventManagerComponent.module.css';

const EventManagerScreenComponent: React.FC = () => {
    const [checkoutDetails, setCheckoutDetails] = useState<CheckoutDetailsUpdate>({});
    const [previousCheckoutDetails, setPreviousCheckoutDetails] = useState(checkoutDetails);
    const {
        event,
        checkoutState,
        pageId,
        setPageId,
        sendMessage,
        isSocketConnected,
        isMerchantConnected,
        isStarted,
        connect,
        disconnect,
    } = useContext(PageContext);

    const onPageIdChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            setPageId(e.target.value);
        },
        [setPageId]
    );

    const sendResponse = useCallback(() => {
        sendMessage({ checkoutDetails });

        // Сохраняем заполненные поля в секциях, чтобы подставить их в форму в следующем событии
        setPreviousCheckoutDetails(prevDetails => {
            return {
                ...prevDetails,
                ...checkoutDetails,
            };
        });

        setCheckoutDetails({});
    }, [sendMessage, checkoutDetails]);

    if (!isStarted) {
        return (
            <div className={styles.connect}>
                <div className={styles.connectInner}>
                    <InputLabel title="Page ID">
                        <Textinput
                            view="default"
                            size="m"
                            type="text"
                            value={pageId}
                            placeholder="page id"
                            onChange={onPageIdChange}
                        />
                    </InputLabel>
                    <Button view="action" size="l" width="max" onClick={connect} className={styles.connectButton}>
                        Подключиться к магазину
                    </Button>
                </div>
            </div>
        );
    }

    let statusMessage: React.ReactNode = 'Ожидание события…';
    if (!isSocketConnected) {
        statusMessage = 'Подключение к серверу…';
    } else if (!isMerchantConnected) {
        statusMessage = 'Подключение к магазину…';
    } else if (event) {
        statusMessage = (
            <>
                Событие: <span className={styles.eventName}>{event}</span>
            </>
        );
    }

    return (
        <>
            <div className={styles.header}>
                <Text typography="control-m" weight="light" className={styles.pageId}>
                    Подключено к Page ID: <span className={styles.pageIdValue}>{pageId}</span>
                </Text>

                <Text typography="headline-xs" weight="light" align="center" className={styles.event}>
                    {statusMessage}
                </Text>

                <div className={styles.disconnect}>
                    <Button view="pseudo" size="s" onClick={disconnect}>
                        Отключиться от магазина
                    </Button>
                </div>
            </div>
            {event && (
                <div className={styles.content}>
                    <div className={styles.state}>
                        <Text typography="headline-l" weight="light" className={styles.sectionTitle}>
                            CheckoutState
                        </Text>
                        <div className={styles.sticky}>
                            <CheckoutState checkoutState={checkoutState} />
                            <Button
                                className={styles.submit}
                                size="l"
                                view="action"
                                onClick={sendResponse}
                                disabled={!isSocketConnected || !isMerchantConnected}
                            >
                                Отправить
                            </Button>
                        </div>
                    </div>
                    <div className={styles.form}>
                        <Text typography="headline-l" weight="light" className={styles.sectionTitle}>
                            CheckoutDetails
                        </Text>
                        <CheckoutDetailsForm
                            defaultState={previousCheckoutDetails}
                            state={checkoutDetails}
                            setState={setCheckoutDetails}
                        />
                    </div>
                </div>
            )}
        </>
    );
};

export default EventManagerScreenComponent;
