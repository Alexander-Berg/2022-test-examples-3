import React, { useContext, useCallback } from 'react';

import TextInput from '../../../../TextInput';
import { Section } from '../../../components/Section';
import { AddSection } from '../../../components/AddSection';
import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';

const MainError: React.FC = () => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const validationErrors = state.validationErrors ?? {};

    const onChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const { value } = e.target;

            changeState({
                ...state,
                validationErrors: {
                    ...validationErrors,
                    error: value ? value : null,
                },
            });
        },
        [state, validationErrors, changeState]
    );

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            validationErrors: {
                ...validationErrors,
                error: '',
            },
        });
    }, [state, validationErrors, changeState]);

    const onDelete = useCallback(() => {
        const newValidationErrors = { ...validationErrors };
        delete newValidationErrors.error;

        changeState({
            ...state,
            validationErrors: newValidationErrors,
        });
    }, [state, changeState, validationErrors]);

    if (!('error' in validationErrors)) {
        return (
            <AddSection inline onClick={onAdd}>
                Основная ошибка
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Основная ошибка" onDelete={onDelete}>
            <TextInput placeholder="Основная ошибка" value={validationErrors.error ?? ''} onChange={onChange} />
        </Section>
    );
};

export default MainError;
