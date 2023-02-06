import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../lib/array';

import { Section } from '../../../components/Section';
import { AddSection } from '../../../components/AddSection';
import { List } from '../../../components/List';
import { Presets } from '../../../components/Presets';

import {
    expressShippingOption,
    deliveryShippingOption,
    pickupShippingOption,
    postShippingOption,
} from '../../../../../presets/shipping-options';
import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';
import { CheckoutOrder, CheckoutShippingOption } from '../../../../../types/checkout-api';

import ShippingOption from './Option';

type ShippingOptionsProps = {
    index: number;
};

const presets = [
    { title: 'Курьер платно', value: expressShippingOption },
    { title: 'Курьер бесплатно', value: deliveryShippingOption },
    { title: 'Самовывоз без времени', value: pickupShippingOption },
    { title: 'Почта платно', value: postShippingOption },
];

const ShippingOptions: React.FC<ShippingOptionsProps> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const orders = state.orders ?? [];
    const order = orders[currentIndex] ?? {};
    const shippingOptions = order?.shippingOptions ?? [];

    const onAddItem = useCallback(() => {
        const updatedOrder: CheckoutOrder = {
            ...order,
            shippingOptions: [...shippingOptions, { id: '' }],
        };
        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [orders, order, shippingOptions, state, changeState, currentIndex]);

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            orders: replaceItem(
                orders,
                {
                    ...order,
                    shippingOptions: [],
                },
                currentIndex
            ),
        });
    }, [state, changeState, order, orders, currentIndex]);

    const onDelete = useCallback(() => {
        const updatedOrder: CheckoutOrder = { ...order };
        delete updatedOrder.shippingOptions;

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [state, order, orders, changeState, currentIndex]);

    const onPresetSelect = useCallback(
        (preset: CheckoutShippingOption) => {
            const updatedOrder: CheckoutOrder = {
                ...order,
                shippingOptions: [...shippingOptions, preset],
            };
            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, currentIndex),
            });
        },
        [orders, order, shippingOptions, state, changeState, currentIndex]
    );

    if (!('shippingOptions' in order)) {
        return (
            <AddSection inline onClick={onAdd}>
                Способы доставки
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Способы доставки" onDelete={onDelete}>
            <List itemTitle="Способ доставки" onAdd={onAddItem}>
                {shippingOptions.map((_, index) => (
                    <ShippingOption key={index} orderIndex={currentIndex} shippingOptionIndex={index} />
                ))}
            </List>
            <Presets presets={presets} onSelect={onPresetSelect} />
        </Section>
    );
};

export default ShippingOptions;
