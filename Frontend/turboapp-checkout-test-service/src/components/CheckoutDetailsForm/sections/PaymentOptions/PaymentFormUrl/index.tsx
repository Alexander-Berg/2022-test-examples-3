import React, { useMemo, useContext, useCallback } from 'react';

import { CheckoutDetailsUpdate } from '../../../../../types/checkout-api';
import { getDefaultTestPaymentFormUrl } from '../../../../../lib/url-builder';

import TextInput from '../../../../TextInput';
import { Button } from '../../../../Button';
import { Section } from '../../../components/Section';
import { AddSection } from '../../../components/AddSection';
import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';

import styles from './PaymentFormUrl.module.css';

const updatePaymentFormUrl = (state: CheckoutDetailsUpdate, paymentFormUrl?: string) => {
    return state.paymentOptions?.map(option => {
        const { type } = option;

        if (type === 'online-external-payments') {
            return typeof paymentFormUrl === 'string' ? { type, data: { paymentFormUrl } } : { type };
        }

        return option;
    }, []);
};

const PaymentFormUrl: React.FC = () => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const { data } = useMemo(() => {
        return state.paymentOptions?.find(({ type }) => type === 'online-external-payments') ?? {};
    }, [state]);
    const { paymentFormUrl } = (data ?? {}) as { paymentFormUrl?: string };

    const onPaymentFormUrlChange = useCallback((paymentFormUrl?: string) => {
        const paymentOptions = updatePaymentFormUrl(state, paymentFormUrl);

        changeState({ ...state, paymentOptions });
    }, [state, changeState]);

    const onAdd = useCallback(() => onPaymentFormUrlChange(''), [onPaymentFormUrlChange]);
    const onDelete = useCallback(() => onPaymentFormUrlChange(), [onPaymentFormUrlChange]);
    const onChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onPaymentFormUrlChange(e.target.value);
        },
        [onPaymentFormUrlChange]
    );
    const onSetDefaultFormUrl = useCallback(() => {
        onPaymentFormUrlChange(getDefaultTestPaymentFormUrl());
    }, [onPaymentFormUrlChange]);

    if (!data) {
        return <AddSection inline onClick={onAdd}>Форма оплаты</AddSection>;
    }

    return (
        <Section title="Форма оплаты" onDelete={onDelete}>
            <div className={styles.section}>
                <TextInput value={paymentFormUrl} onChange={onChange} placeholder="URL формы оплаты" />
                <Button view="default" size="m" onClick={onSetDefaultFormUrl}>
                    Стандартный URL формы оплаты
                </Button>
            </div>
        </Section>
    );
};

export default PaymentFormUrl;
