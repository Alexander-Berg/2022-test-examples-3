import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../../lib/array';

import TextInput from '../../../../../TextInput';

import { Section } from '../../../../components/Section';
import { AddSection } from '../../../../components/AddSection';

import { CheckoutDetailsFormContext } from '../../../../CheckoutDetailsFormProvider';
import { CheckoutOrderErrors } from '../../../../../../types/checkout-api';

type OrderIdProps = {
    index: number;
};

const OrderId: React.FC<OrderIdProps> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const orders = state.validationErrors?.orders ?? [];
    const order = orders[currentIndex] ?? {};

    const onChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const updatedOrder: CheckoutOrderErrors = {
                ...state.validationErrors?.orders?.[currentIndex],
                id: e.target.value || undefined,
            };

            changeState({
                ...state,
                validationErrors: {
                    ...state.validationErrors,
                    orders: replaceItem(state.orders ?? [], updatedOrder, currentIndex),
                },
            });
        },
        [state, changeState, currentIndex]
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
                        id: `order-${currentIndex + 1}`,
                    },
                    currentIndex
                ),
            },
        });
    }, [state, changeState, order, orders, currentIndex]);

    const onDelete = useCallback(() => {
        const updatedOrder: CheckoutOrderErrors = { ...state.validationErrors?.orders?.[currentIndex] };
        delete updatedOrder.id;

        changeState({
            ...state,
            validationErrors: {
                ...state.validationErrors,
                orders: replaceItem(state.validationErrors?.orders ?? [], updatedOrder, currentIndex),
            },
        });
    }, [state, changeState, currentIndex]);

    if (!('id' in order)) {
        return (
            <AddSection inline onClick={onAdd}>
                ?????????????????????????? ????????????
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="?????????????????????????? ????????????" onDelete={onDelete}>
            <TextInput value={order.id} onChange={onChange} placeholder="?????????????????????????? ????????????" />
        </Section>
    );
};

export default OrderId;
