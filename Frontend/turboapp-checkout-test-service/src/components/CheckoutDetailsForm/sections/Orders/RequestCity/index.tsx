import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../lib/array';

import Checkbox from '../../../../Checkbox';
import { Section } from '../../../components/Section';
import { AddSection } from '../../../components/AddSection';

import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';
import { CheckoutOrder } from '../../../../../types/checkout-api';

type RequestCityProps = {
    index: number;
};

const RequestCity: React.FC<RequestCityProps> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const orders = state.orders ?? [];
    const order = orders[currentIndex] ?? {};

    const isChecked = Boolean(order.requestCity);
    const onChange = useCallback(() => {
        const updatedOrder: CheckoutOrder = {
            ...order,
            requestCity: !order.requestCity,
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [state, order, orders, changeState, currentIndex]);

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            orders: replaceItem(
                orders,
                {
                    ...order,
                    requestCity: true,
                },
                currentIndex
            ),
        });
    }, [state, changeState, order, orders, currentIndex]);

    const onDelete = useCallback(() => {
        const updatedOrder: CheckoutOrder = { ...order };
        delete updatedOrder.requestCity;

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [state, order, orders, changeState, currentIndex]);

    if (!('requestCity' in order)) {
        return (
            <AddSection inline onClick={onAdd}>
                Город
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Город" onDelete={onDelete}>
            <Checkbox checked={isChecked} onChange={onChange} label="Запросить ввод города" />
        </Section>
    );
};

export default RequestCity;
