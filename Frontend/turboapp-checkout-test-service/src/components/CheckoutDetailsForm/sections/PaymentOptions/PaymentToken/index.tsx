import React, { useMemo, useContext, useCallback, useState } from 'react';

import { CheckoutDetailsUpdate } from '../../../../../types/checkout-api';

import TextInput from '../../../../TextInput';
import { Button } from '../../../../Button';
import { Section } from '../../../components/Section';
import { AddSection } from '../../../components/AddSection';
import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';
import { TokenRequestModal } from './TokenRequestModal';

import styles from './PaymentToken.module.css';

const updatePaymentToken = (state: CheckoutDetailsUpdate, paymentToken?: string) => {
    return state.paymentOptions?.map(option => {
        const { type } = option;

        if (type === 'yandex-payments') {
            return typeof paymentToken === 'string' ? { type, data: { paymentToken } } : { type };
        }

        return option;
    }, []);
};

const PaymentToken: React.FC = () => {
    const { defaultState, state, changeState } = useContext(CheckoutDetailsFormContext);
    const [visible, setModalVisible] = useState(false);

    const { data } = useMemo(() => state.paymentOptions?.find(({ type }) => type === 'yandex-payments') ?? {}, [state]);
    const { paymentToken } = (data ?? {}) as { paymentToken?: string };

    const onTokenChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const paymentOptions = updatePaymentToken(state, e.target.value);

            changeState({ ...state, paymentOptions });
        },
        [state, changeState]
    );

    const onAdd = useCallback(() => {
        const paymentOptions = updatePaymentToken(state, '');

        changeState({ ...state, paymentOptions });
    }, [state, changeState]);

    const onDelete = useCallback(() => {
        const paymentOptions = updatePaymentToken(state);

        changeState({ ...state, paymentOptions });
    }, [state, changeState]);

    const onRequestClick = useCallback(() => {
        setModalVisible(true);
    }, []);

    const onTokenReceived = useCallback(
        (paymentToken: string) => {
            const paymentOptions = updatePaymentToken(state, paymentToken);

            changeState({ ...state, paymentOptions });
            setModalVisible(false);
        },
        [state, changeState]
    );

    const onCloseModal = useCallback(() => {
        setModalVisible(false);
    }, []);

    if (!data) {
        return <AddSection inline onClick={onAdd}>Токен оплаты</AddSection>;
    }

    return (
        <Section title="Токен оплаты" onDelete={onDelete}>
            <div className={styles.section}>
                <TextInput value={paymentToken} onChange={onTokenChange} placeholder="Токен" />
                <Button view="default" size="m" onClick={onRequestClick}>
                    Получить токен
                </Button>
            </div>
            {visible && (
                <TokenRequestModal
                    items={state.orders?.[0]?.cartItems ?? defaultState.orders?.[0]?.cartItems ?? []}
                    onTokenReceived={onTokenReceived}
                    onClose={onCloseModal}
                />
            )}
        </Section>
    );
};

export default PaymentToken;
