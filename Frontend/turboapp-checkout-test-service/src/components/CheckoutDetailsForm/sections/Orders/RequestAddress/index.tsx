import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../lib/array';

import Checkbox from '../../../../Checkbox';
import { Section } from '../../../components/Section';
import { AddSection } from '../../../components/AddSection';

import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';
import { CheckoutOrder } from '../../../../../types/checkout-api';

type RequestAddressProps = {
    index: number;
};

const RequestAddress: React.FC<RequestAddressProps> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const orders = state.orders ?? [];
    const order = orders[currentIndex] ?? {};

    const isChecked = Boolean(order.requestShippingAddress);
    const onChange = useCallback(() => {
        const updatedOrder: CheckoutOrder = {
            ...order,
            requestShippingAddress: !order.requestShippingAddress,
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
                    requestShippingAddress: true,
                },
                currentIndex
            ),
        });
    }, [state, changeState, order, orders, currentIndex]);

    const onDelete = useCallback(() => {
        const updatedOrder: CheckoutOrder = { ...order };
        delete updatedOrder.requestShippingAddress;

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [state, order, orders, changeState, currentIndex]);

    if (!('requestShippingAddress' in order)) {
        return (
            <AddSection inline onClick={onAdd}>
                Адрес доставки
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Адрес доставки" onDelete={onDelete}>
            <Checkbox checked={isChecked} onChange={onChange} label="Запросить ввод адреса" />
        </Section>
    );
};

export default RequestAddress;
