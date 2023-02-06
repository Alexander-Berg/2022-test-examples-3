import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../lib/array';

import { Section } from '../../../components/Section';
import { AddSection } from '../../../components/AddSection';
import { List } from '../../../components/List';
import { Presets } from '../../../components/Presets';

import {
    fullInfoItem,
    multiplyImageItem,
    withoutAmountItem,
    withoutImageItem,
} from '../../../../../presets/cart-items';
import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';
import { CheckoutCartItem, CheckoutOrder } from '../../../../../types/checkout-api';

import CartItem from './Item';

type CartItemsProps = {
    index: number;
};

const presets = [
    { title: 'Полная информация о товаре', value: fullInfoItem },
    { title: 'Товар с несколькими картинками', value: multiplyImageItem },
    { title: 'Товар без цены', value: withoutAmountItem },
    { title: 'Товар без картинки', value: withoutImageItem },
];

const CartItems: React.FC<CartItemsProps> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const orders = state.orders ?? [];
    const order = orders[currentIndex] ?? {};
    const cartItems = order?.cartItems ?? [];

    const onAddItem = useCallback(() => {
        const updatedOrder: CheckoutOrder = {
            ...order,
            cartItems: [...cartItems, { title: '', amount: {}, count: 1, image: [] }],
        };
        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [orders, order, cartItems, state, changeState, currentIndex]);

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            orders: replaceItem(
                orders,
                {
                    ...order,
                    cartItems: [],
                },
                currentIndex
            ),
        });
    }, [state, changeState, order, orders, currentIndex]);

    const onDelete = useCallback(() => {
        const updatedOrder: CheckoutOrder = { ...order };
        delete updatedOrder.cartItems;

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [state, order, orders, changeState, currentIndex]);

    const onPresetSelect = useCallback(
        (preset: CheckoutCartItem) => {
            const updatedOrder: CheckoutOrder = {
                ...order,
                cartItems: [...cartItems, preset],
            };
            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, currentIndex),
            });
        },
        [orders, order, cartItems, state, changeState, currentIndex]
    );

    if (!('cartItems' in order)) {
        return (
            <AddSection inline onClick={onAdd}>
                Товары
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Товары" onDelete={onDelete}>
            <List itemTitle="Товар" onAdd={onAddItem}>
                {cartItems.map((_, index) => (
                    <CartItem key={index} orderIndex={currentIndex} cartItemIndex={index} />
                ))}
            </List>
            <Presets presets={presets} onSelect={onPresetSelect} />
        </Section>
    );
};

export default CartItems;
