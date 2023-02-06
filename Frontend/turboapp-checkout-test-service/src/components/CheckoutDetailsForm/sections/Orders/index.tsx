import React, { useContext, useCallback, useMemo, useState } from 'react';

import { CheckoutOrder } from '../../../../types/checkout-api';
import {
    fullOrder,
    orderWithCartDetails,
    orderWithCartItems,
    orderWithDateTimeOptions,
    orderWithShippingOptions,
} from '../../../../presets/orders';

import { Section } from '../../components/Section';
import { AddSection } from '../../components/AddSection';
import { TabsSection } from '../../components/TabsSection';
import { AddButton } from '../../components/AddButton';
import { Presets } from '../../components/Presets';
import { CheckoutDetailsFormContext } from '../../CheckoutDetailsFormProvider';

import DateTimeOptions from './DateTimeOptions';
import Id from './Id';
import PickupOptions from './PickupOptions';
import RequestAddress from './RequestAddress';
import CartItems from './CartItems';
import CartDetails from './CartDetails';
import RequestCity from './RequestCity';
import ShippingOptions from './ShippingOptions';

const presets = [
    { title: 'Полный заказ', value: fullOrder },
    { title: 'Заказ с информацией о корзине', value: orderWithCartDetails },
    { title: 'Заказ с товарами', value: orderWithCartItems },
    { title: 'Заказ с временем доставки', value: orderWithDateTimeOptions },
    { title: 'Заказ с доставкой', value: orderWithShippingOptions },
];

const Orders: React.FC = () => {
    const { defaultState, state, changeState } = useContext(CheckoutDetailsFormContext);

    const [activeTab, setActiveTab] = useState('0');
    const maxIndex = (state.orders?.length ?? 0) - 1;
    const index = Math.min(Number(activeTab), maxIndex);

    const onAdd = useCallback(() => {
        changeState({ ...state, orders: defaultState.orders ?? [{ id: 'order-1' }] });
    }, [defaultState, state, changeState]);

    const onDelete = useCallback(() => {
        const newState = { ...state };
        delete newState.orders;

        changeState(newState);
    }, [state, changeState]);

    const onOrderAdd = useCallback(() => {
        changeState({
            ...state,
            orders: state.orders?.concat({
                id: `order-${state.orders?.length + 1}`,
            }),
        });

        setActiveTab(String(state.orders?.length));
    }, [state, changeState]);

    const onOrderDelete = useCallback(
        (index: number) => {
            changeState({
                ...state,
                orders: [...(state.orders?.slice(0, index) ?? []), ...(state.orders?.slice(index + 1) ?? [])],
            });
        },
        [state, changeState]
    );

    const onPresetSelect = useCallback(
        (preset: CheckoutOrder) => {
            changeState({
                ...state,
                orders: [...(state.orders ?? []), preset],
            });
        },
        [state, changeState]
    );

    const tabs = useMemo(() => {
        return Array(state.orders?.length || 0)
            .fill(0)
            .map((_, index) => ({
                id: String(index),
                title: `Заказ #${index + 1}`,
                onClose: () => onOrderDelete(index),
            }));
    }, [state, onOrderDelete]);

    if (!('orders' in state)) {
        return <AddSection onClick={onAdd}>Заказы</AddSection>;
    }

    return (
        <Section title="Заказы" onDelete={onDelete}>
            <Presets presets={presets} onSelect={onPresetSelect} />
            {index === -1 && (
                <AddButton size="s" onClick={onOrderAdd}>
                    Заказ
                </AddButton>
            )}
            {index !== -1 && (
                <>
                    <TabsSection tabs={tabs} activeTab={String(index)} onChange={setActiveTab} onAdd={onOrderAdd} />

                    <Id index={index} />
                    <CartItems index={index} />
                    <CartDetails index={index} />
                    <RequestCity index={index} />
                    <RequestAddress index={index} />
                    <ShippingOptions index={index} />
                    <PickupOptions index={index} />
                    <DateTimeOptions index={index} />
                </>
            )}
        </Section>
    );
};

export default Orders;
