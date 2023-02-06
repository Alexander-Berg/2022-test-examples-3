import React, { useContext, useCallback, useMemo } from 'react';

import Checkbox from '../../../Checkbox';

import { Section } from '../../components/Section';
import { AddSection } from '../../components/AddSection';

import { CheckoutDetailsFormContext } from '../../CheckoutDetailsFormProvider';

import styles from './PayerDetails.module.css';

const fieldLabelMap: Record<string, string> = {
    name: 'Имя',
    phone: 'Телефон',
    email: 'Email',
};

const ContactField: React.FC<{ type: string }> = ({ type: currentType }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const details = useMemo(() => state.payerDetails?.find(detail => currentType === detail?.type), [
        currentType,
        state.payerDetails,
    ]);
    const isRequired = details?.require;

    const onChange = useCallback(() => {
        const isChecked = !details;

        if (isChecked) {
            return changeState({
                ...state,
                payerDetails: [...(state.payerDetails ?? []), { type: currentType, require: false }],
            });
        }

        changeState({
            ...state,
            payerDetails: state.payerDetails?.filter(detail => detail?.type !== currentType),
        });
    }, [state, changeState, currentType, details]);

    const onRequireChange = useCallback(() => {
        const require = !details?.require;

        changeState({
            ...state,
            payerDetails: state.payerDetails?.map(detail =>
                detail?.type === currentType ? { type: currentType, require } : detail
            ),
        });
    }, [state, changeState, currentType, details]);

    return (
        <>
            <Checkbox
                className={styles.type}
                checked={Boolean(details)}
                onChange={onChange}
                label={fieldLabelMap[currentType]}
            />
            {details && (
                <Checkbox
                    className={styles.details}
                    checked={isRequired}
                    onChange={onRequireChange}
                    label="Обязательное поле"
                />
            )}
        </>
    );
};

const PayerDetails: React.FC = () => {
    const { defaultState, state, changeState } = useContext(CheckoutDetailsFormContext);

    const fields = useMemo<string[]>(() => ['name', 'phone', 'email'], []);

    const onAdd = useCallback(() => {
        changeState({ ...state, payerDetails: defaultState.payerDetails ?? [] });
    }, [defaultState, state, changeState]);

    const onDelete = useCallback(() => {
        const newState = { ...state };
        delete newState.payerDetails;

        changeState(newState);
    }, [state, changeState]);

    if (!('payerDetails' in state)) {
        return <AddSection onClick={onAdd}>Контакты пользователя</AddSection>;
    }

    return (
        <Section title="Контакты пользователя" onDelete={onDelete}>
            <div className={styles.section}>
                {fields.map(type => (
                    <ContactField key={type} type={type} />
                ))}
            </div>
        </Section>
    );
};

export default PayerDetails;
