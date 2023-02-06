import React, { useMemo, useContext, useCallback } from 'react';

import Checkbox from '../../../Checkbox';
import { Section } from '../../components/Section';
import { AddSection } from '../../components/AddSection';
import { CheckoutDetailsFormContext } from '../../CheckoutDetailsFormProvider';
import PaymentToken from './PaymentToken';
import PaymentFormUrl from './PaymentFormUrl';

import styles from './PaymentOptions.module.css';

const options: Array<string> = ['offline-cash', 'offline-card', 'yandex-payments', 'online-external-payments'];
const optionLabelMap: Record<string, string> = {
    'offline-cash': 'Наличными курьеру',
    'offline-card': 'Картой курьеру',
    'yandex-payments': 'Яндекс оплата',
    'online-external-payments': 'Онлайн оплата',
};

const PaymentOption: React.FC<{ type: string }> = ({ type: currentType }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const isChecked = useMemo(() => state.paymentOptions?.some(item => item?.type === currentType), [
        currentType,
        state.paymentOptions,
    ]);

    const onChange = useCallback(() => {
        if (isChecked) {
            changeState({
                ...state,
                paymentOptions: state.paymentOptions?.filter(option => option?.type !== currentType),
            });

            return;
        }

        changeState({
            ...state,
            paymentOptions: [...(state.paymentOptions ?? []), { type: currentType }],
        });
    }, [state, changeState, currentType, isChecked]);

    return (
        <>
            <Checkbox
                className={styles.type}
                checked={isChecked}
                onChange={onChange}
                label={optionLabelMap[currentType]}
            />
            {isChecked && currentType === 'yandex-payments' && <PaymentToken />}
            {isChecked && currentType === 'online-external-payments' && <PaymentFormUrl />}
        </>
    );
};

const PaymentOptions: React.FC = () => {
    const { defaultState, state, changeState } = useContext(CheckoutDetailsFormContext);

    const onAdd = useCallback(() => {
        changeState({ ...state, paymentOptions: defaultState.paymentOptions ?? [] });
    }, [defaultState, state, changeState]);

    const onDelete = useCallback(() => {
        const newState = { ...state };
        delete newState.paymentOptions;

        changeState(newState);
    }, [state, changeState]);

    if (!('paymentOptions' in state)) {
        return <AddSection onClick={onAdd}>Способы оплаты</AddSection>;
    }

    return (
        <Section title="Способы оплаты" onDelete={onDelete}>
            <div className={styles.section}>
                {options.map(type => (
                    <PaymentOption key={type} type={type} />
                ))}
            </div>
        </Section>
    );
};

export default PaymentOptions;
