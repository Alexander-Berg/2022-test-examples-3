import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../../lib/array';

import TextInput from '../../../../../TextInput';

import { DeleteButton } from '../../../../components/DeleteButton';

import { CheckoutDetailsFormContext } from '../../../../CheckoutDetailsFormProvider';
import { CheckoutCartItem, CheckoutOrder, CheckoutCartItemImage } from '../../../../../../types/checkout-api';

import styles from './CartItemImage.module.css';

type CartItemProps = {
    cartItemImageIndex: number;
    cartItemIndex: number;
    orderIndex: number;
};

const CartItemImage: React.FC<CartItemProps> = ({ cartItemImageIndex, cartItemIndex, orderIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);
    const orders = state.orders ?? [];
    const order = orders[orderIndex];
    const cartItems = order?.cartItems ?? [];
    const cartItem = cartItems[cartItemIndex];
    const cartItemImages = cartItems[cartItemIndex]?.image ?? [];
    const cartItemImage = cartItemImages[cartItemImageIndex];

    const { url, width, height } = cartItemImage ?? {};

    const onUpdateImage = useCallback(
        (updatedImage: CheckoutCartItemImage) => {
            const updatedCartItem: CheckoutCartItem = {
                ...cartItem,
                image: replaceItem(cartItemImages, updatedImage, cartItemImageIndex),
            };
            const updatedOrder: CheckoutOrder = {
                ...order,
                cartItems: replaceItem(cartItems, updatedCartItem, cartItemIndex),
            };

            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, orderIndex),
            });
        },
        [
            orders,
            order,
            cartItem,
            cartItems,
            cartItemImages,
            orderIndex,
            cartItemIndex,
            cartItemImageIndex,
            state,
            changeState,
        ]
    );

    const onUrlChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateImage({
                ...cartItemImage,
                url: e.target.value || undefined,
            });
        },
        [cartItemImage, onUpdateImage]
    );

    const onWidthChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateImage({
                ...cartItemImage,
                width: Number(e.target.value) || undefined,
            });
        },
        [cartItemImage, onUpdateImage]
    );

    const onHeightChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateImage({
                ...cartItemImage,
                height: Number(e.target.value) || undefined,
            });
        },
        [cartItemImage, onUpdateImage]
    );

    const onDelete = useCallback(() => {
        const updatedCartItem: CheckoutCartItem = {
            ...cartItem,
            image: cartItemImages.filter((_, index) => index !== cartItemImageIndex),
        };
        const updatedOrder: CheckoutOrder = {
            ...order,
            cartItems: replaceItem(cartItems, updatedCartItem, cartItemIndex),
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [
        orders,
        order,
        cartItems,
        cartItem,
        cartItemImages,
        orderIndex,
        cartItemIndex,
        cartItemImageIndex,
        state,
        changeState,
    ]);

    return (
        <div className={styles.image}>
            <TextInput value={url} placeholder="url" className={styles.url} onChange={onUrlChange} />
            <TextInput className={styles.width} value={width} type="number" onChange={onWidthChange} hasClear={false} />
            <span className={styles.separator}>x</span>
            <TextInput
                className={styles.height}
                value={height}
                type="number"
                onChange={onHeightChange}
                hasClear={false}
            />
            <DeleteButton onClick={onDelete} />
        </div>
    );
};

export default CartItemImage;
