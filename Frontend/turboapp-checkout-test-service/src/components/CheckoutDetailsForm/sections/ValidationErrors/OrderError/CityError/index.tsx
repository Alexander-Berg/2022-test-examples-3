import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../../lib/array';

import TextInput from '../../../../../TextInput';
import { Section } from '../../../../components/Section';
import { AddSection } from '../../../../components/AddSection';

import { CheckoutDetailsFormContext } from '../../../../CheckoutDetailsFormProvider';
import { CheckoutOrderErrors } from '../../../../../../types/checkout-api';

type CityErrorProps = {
    index: number;
};

const CityError: React.FC<CityErrorProps> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const orders = state.validationErrors?.orders ?? [];
    const order = orders[currentIndex] ?? {};

    const onChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const updatedOrder: CheckoutOrderErrors = {
                ...order,
                city: e.target.value,
            };

            changeState({
                ...state,
                validationErrors: {
                    ...state.validationErrors,
                    orders: replaceItem(orders, updatedOrder, currentIndex),
                },
            });
        },
        [state, order, orders, changeState, currentIndex]
    );

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            validationErrors: {
                ...state.validationErrors,
                orders: replaceItem(
                    orders,
                    {
                        ...order,
                        city: '',
                    },
                    currentIndex
                ),
            },
        });
    }, [state, changeState, order, orders, currentIndex]);

    const onDelete = useCallback(() => {
        const updatedOrder: CheckoutOrderErrors = { ...order };
        delete updatedOrder.city;

        changeState({
            ...state,
            validationErrors: {
                ...state.validationErrors,
                orders: replaceItem(orders, updatedOrder, currentIndex),
            },
        });
    }, [state, order, orders, changeState, currentIndex]);

    if (!('city' in order)) {
        return (
            <AddSection inline onClick={onAdd}>
                Ошибка в городе
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Ошибка в городе" onDelete={onDelete}>
            <TextInput value={order?.city ?? ''} placeholder="Ошибка в городе" onChange={onChange} />
        </Section>
    );
};

export default CityError;
