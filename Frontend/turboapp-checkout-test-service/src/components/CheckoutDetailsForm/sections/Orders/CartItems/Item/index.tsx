import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../../lib/array';

import TextInput from '../../../../../TextInput';
import { InputLabel } from '../../../../../InputLabel';
import Price from '../../../../../Price';

import { AddButton } from '../../../../components/AddButton';
import { ListItem } from '../../../../components/List';

import { CheckoutDetailsFormContext } from '../../../../CheckoutDetailsFormProvider';
import { CheckoutCartItem, CheckoutOrder, CheckoutPrice } from '../../../../../../types/checkout-api';

import CartItemImage from '../Image';

import styles from './CartItem.module.css';

type CartItemProps = {
    cartItemIndex: number;
    orderIndex: number;
};

const CartItem: React.FC<CartItemProps> = ({ cartItemIndex, orderIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);
    const orders = state.orders ?? [];
    const order = orders[orderIndex];
    const cartItems = order?.cartItems ?? [];
    const cartItem = cartItems[cartItemIndex];

    const title = cartItem?.title;
    const price = cartItem?.amount;
    const count = cartItem?.count;

    const onUpdateItem = useCallback(
        (updatedItem: CheckoutCartItem) => {
            const updatedOrder: CheckoutOrder = {
                ...order,
                cartItems: replaceItem(cartItems, updatedItem, cartItemIndex),
            };

            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, orderIndex),
            });
        },
        [orders, order, cartItems, orderIndex, cartItemIndex, state, changeState]
    );

    const onTitleChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateItem({
                ...cartItem,
                title: e.target.value,
            });
        },
        [cartItem, onUpdateItem]
    );

    const onPriceChange = useCallback((amount?: CheckoutPrice) => {
        onUpdateItem({
            ...cartItem,
            amount,
        });
    }, [cartItem, onUpdateItem]);

    const onCountChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateItem({
                ...cartItem,
                count: Number(e.target.value),
            });
        },
        [cartItem, onUpdateItem]
    );

    const onDelete = useCallback(() => {
        const filteredItems = cartItems.filter((_, index) => index !== cartItemIndex);
        const updatedOrder: CheckoutOrder = {
            ...order,
            cartItems: filteredItems.length > 0 ? filteredItems : undefined,
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [orders, order, cartItems, cartItemIndex, orderIndex, state, changeState]);

    const onImageAdd = useCallback(() => {
        onUpdateItem({
            ...cartItem,
            image: [...(cartItem?.image ?? []), { url: '' }],
        });
    }, [cartItem, onUpdateItem]);

    return (
        <ListItem title="Товар" onDelete={onDelete} className={styles.item}>
            <InputLabel title="Название товара">
                <TextInput value={title} placeholder="Название" onChange={onTitleChange} />
            </InputLabel>
            <InputLabel title="Цена">
                <Price price={price} onChange={onPriceChange} />
            </InputLabel>
            <InputLabel title="Количество">
                <TextInput value={count} placeholder="Количество" type="number" onChange={onCountChange} />
            </InputLabel>
            <div>
                <InputLabel title="Картинки" />
                {cartItem?.image?.map((_, index) => (
                    <CartItemImage
                        key={index}
                        cartItemImageIndex={index}
                        cartItemIndex={cartItemIndex}
                        orderIndex={orderIndex}
                    />
                ))}
            </div>
            <div>
                <AddButton size="s" onClick={onImageAdd}>
                    Картинка
                </AddButton>
            </div>
        </ListItem>
    );
};

export default CartItem;
