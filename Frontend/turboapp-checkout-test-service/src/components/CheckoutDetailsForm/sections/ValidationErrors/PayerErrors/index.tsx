import React, { useContext, useCallback } from 'react';
import { CheckoutValidationErrors } from '../../../../../types/checkout-api';

import TextInput from '../../../../TextInput';
import { InputLabel } from '../../../../InputLabel';

import { Section } from '../../../components/Section';
import { DeleteButton } from '../../../components/DeleteButton';
import { AddSection } from '../../../components/AddSection';

import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';

import styles from './PayerErrors.module.css';

type Fields = 'name' | 'email' | 'phone';

const PayerError: React.FC<{ placeholder: string; type: Fields }> = ({ placeholder, type }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const validationErrors = state.validationErrors ?? {};
    const payerErrors = validationErrors.payerDetails ?? {};

    const onChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            changeState({
                ...state,
                validationErrors: {
                    ...state.validationErrors,
                    payerDetails: {
                        ...state?.validationErrors?.payerDetails,
                        [type]: e.target.value,
                    },
                },
            });
        },
        [state, changeState, type]
    );

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            validationErrors: {
                ...validationErrors,
                payerDetails: {
                    ...validationErrors.payerDetails,
                    [type]: '',
                },
            },
        });
    }, [state, validationErrors, changeState, type]);

    const onDelete = useCallback(() => {
        const updatedPayerErrors = { ...payerErrors };
        delete updatedPayerErrors[type];

        changeState({
            ...state,
            validationErrors: {
                ...state.validationErrors,
                payerDetails: updatedPayerErrors,
            },
        });
    }, [state, changeState, payerErrors, type]);

    if (!(type in payerErrors)) {
        return (
            <AddSection inline onClick={onAdd} className={styles.addSection}>
                {placeholder}
            </AddSection>
        );
    }

    return (
        <InputLabel title={placeholder} className={styles.error}>
            <div className={styles.wrapper}>
                <TextInput
                    className={styles.input}
                    placeholder={placeholder}
                    value={payerErrors[type] ?? ''}
                    onChange={onChange}
                />
                <DeleteButton onClick={onDelete} />
            </div>
        </InputLabel>
    );
};

const PayerErrors: React.FC = () => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const validationErrors = state.validationErrors ?? {};

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            validationErrors: {
                ...validationErrors,
                payerDetails: {},
            },
        });
    }, [state, validationErrors, changeState]);

    const onDelete = useCallback(() => {
        const validationErrors: CheckoutValidationErrors = { ...state.validationErrors };
        delete validationErrors.payerDetails;

        changeState({
            ...state,
            validationErrors,
        });
    }, [state, changeState]);

    if (!('payerDetails' in validationErrors)) {
        return (
            <AddSection inline onClick={onAdd}>
                Ошибки заполнения контактов
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Ошибки заполнения контактов" onDelete={onDelete}>
            <PayerError placeholder="Ошибка в имени" type="name" />
            <PayerError placeholder="Ошибка в телефоне" type="phone" />
            <PayerError placeholder="Ошибка в email" type="email" />
        </Section>
    );
};

export default PayerErrors;
