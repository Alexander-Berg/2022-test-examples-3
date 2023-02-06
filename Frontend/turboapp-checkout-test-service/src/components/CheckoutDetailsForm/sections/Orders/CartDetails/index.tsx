import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../lib/array';

import { Section } from '../../../components/Section';
import { AddButton } from '../../../components/AddButton';
import { AddSection } from '../../../components/AddSection';
import { Presets } from '../../../components/Presets';

import { productsAmount, checkoutWeight, discount } from '../../../../../presets/cart-details';
import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';
import { CheckoutCartDetail, CheckoutOrder } from '../../../../../types/checkout-api';

import CartDetail from './Detail';

type CartDetailsProps = {
    index: number;
};

const presets = [
    { title: 'Количество товаров', value: productsAmount },
    { title: 'Вес заказа', value: checkoutWeight },
    { title: 'Скидка', value: discount },
];

const CartDetails: React.FC<CartDetailsProps> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const orders = state.orders ?? [];
    const order = orders[currentIndex] ?? {};
    const cartDetails = order?.cartDetails ?? [];

    const onAddItem = useCallback(() => {
        const updatedOrder: CheckoutOrder = {
            ...order,
            cartDetails: [...cartDetails, { label: '' }],
        };
        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [orders, order, cartDetails, state, changeState, currentIndex]);

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            orders: replaceItem(
                orders,
                {
                    ...order,
                    cartDetails: [],
                },
                currentIndex
            ),
        });
    }, [state, changeState, order, orders, currentIndex]);

    const onDelete = useCallback(() => {
        const updatedOrder: CheckoutOrder = { ...order };
        delete updatedOrder.cartDetails;

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [state, order, orders, changeState, currentIndex]);

    const onPresetSelect = useCallback(
        (preset: CheckoutCartDetail) => {
            const updatedOrder: CheckoutOrder = {
                ...order,
                cartDetails: [...cartDetails, preset],
            };
            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, currentIndex),
            });
        },
        [orders, order, cartDetails, state, changeState, currentIndex]
    );

    if (!('cartDetails' in order)) {
        return (
            <AddSection inline onClick={onAdd}>
                Дополнительная информация о корзине (вес товаров и тд)
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Дополнительная информация о корзине" onDelete={onDelete}>
            {cartDetails.map((_, index) => (
                <CartDetail key={index} orderIndex={currentIndex} cartDetailIndex={index} />
            ))}
            <AddButton size="s" onClick={onAddItem}>
                Добавить
            </AddButton>
            <Presets presets={presets} onSelect={onPresetSelect} />
        </Section>
    );
};

export default CartDetails;
