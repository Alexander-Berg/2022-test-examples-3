import React, { useContext, useCallback } from 'react';
import { CheckoutValidationErrors } from '../../../../types/checkout-api';
import { mainErrors, payerErrors, orderErrors } from '../../../../presets/validation-errors';

import { Section } from '../../components/Section';
import { AddSection } from '../../components/AddSection';
import { CheckoutDetailsFormContext } from '../../CheckoutDetailsFormProvider';
import { Presets } from '../../components/Presets';

import MainError from './MainError';
import PayerErrors from './PayerErrors';
import OrderError from './OrderError';

const presets = [
    { title: 'Основная ошибка', value: mainErrors },
    { title: 'Ошибка контактов', value: payerErrors },
    { title: 'Ошибка в заказе', value: orderErrors },
];

const ValidationErrors: React.FC = () => {
    const { defaultState, state, changeState } = useContext(CheckoutDetailsFormContext);

    const onAdd = useCallback(() => {
        changeState({ ...state, validationErrors: defaultState.validationErrors ?? {} });
    }, [defaultState, state, changeState]);

    const onDelete = useCallback(() => {
        const newState = { ...state };
        delete newState.validationErrors;

        changeState(newState);
    }, [state, changeState]);

    const onPresetSelect = useCallback(
        (preset: CheckoutValidationErrors) => {
            const updatedErrors: CheckoutValidationErrors = {
                ...state.validationErrors,
                ...preset,
            };

            changeState({
                ...state,
                validationErrors: {
                    ...state.validationErrors,
                    ...updatedErrors,
                },
            });
        },
        [state, changeState]
    );

    if (!('validationErrors' in state)) {
        return <AddSection onClick={onAdd}>Ошибки</AddSection>;
    }

    return (
        <Section title="Ошибки" onDelete={onDelete}>
            <Presets presets={presets} onSelect={onPresetSelect} />
            <MainError />
            <PayerErrors />
            <OrderError />
        </Section>
    );
};

export default ValidationErrors;
