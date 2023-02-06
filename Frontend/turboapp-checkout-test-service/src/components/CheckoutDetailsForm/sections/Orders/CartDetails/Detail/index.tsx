import React, { useContext, useMemo, useCallback } from 'react';

import { replaceItem } from '../../../../../../lib/array';

import TextInput from '../../../../../TextInput';
import Checkbox from '../../../../../Checkbox';
import Price from '../../../../../Price';

import { DeleteButton } from '../../../../components/DeleteButton';
import { CheckoutDetailsFormContext } from '../../../../CheckoutDetailsFormProvider';
import { CheckoutCartDetail, CheckoutOrder, CheckoutPrice } from '../../../../../../types/checkout-api';

import styles from './CartDetail.module.css';

type CartDetailProps = {
    orderIndex: number;
    cartDetailIndex: number;
};

const CartDetail: React.FC<CartDetailProps> = ({ orderIndex, cartDetailIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);
    const orders = state.orders ?? [];
    const order = orders[orderIndex];
    const cartDetails = order?.cartDetails ?? [];
    const cartDetail = cartDetails[cartDetailIndex] ?? {};

    const { label } = cartDetail ?? {};
    const isPrice = 'amount' in cartDetail;
    const value = useMemo(() => {
        if ('value' in cartDetail && cartDetail.value) {
            return cartDetail.value;
        } else if ('amount' in cartDetail) {
            return cartDetail.amount;
        }
        return '';
    }, [cartDetail]);

    const onUpdateDetail = useCallback(
        (updatedDetail: CheckoutCartDetail) => {
            const updatedOrder: CheckoutOrder = {
                ...order,
                cartDetails: replaceItem(cartDetails, updatedDetail, cartDetailIndex),
            };

            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, orderIndex),
            });
        },
        [orders, order, cartDetails, orderIndex, cartDetailIndex, state, changeState]
    );

    const onLabelChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const { value } = e.target;

            const updatedDetail: CheckoutCartDetail = {
                ...cartDetail,
                ...{ label: value },
            };

            onUpdateDetail(updatedDetail);
        },
        [onUpdateDetail, cartDetail]
    );

    const onPriceChange = useCallback((amount?: CheckoutPrice) => {
        const updatedDetail: CheckoutCartDetail = {
            ...cartDetail,
            amount
        };

        onUpdateDetail(updatedDetail);
    }, [onUpdateDetail, cartDetail]);

    const onValueChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const { value } = e.target;

            const updatedDetail: CheckoutCartDetail = {
                ...cartDetail,
                value
            };

            onUpdateDetail(updatedDetail);
        },
        [onUpdateDetail, cartDetail]
    );

    const onTypeChange = useCallback(() => {
        const { label } = cartDetail ?? {};
        const updatedDetail: CheckoutCartDetail = {
            label,
            ...('amount' in cartDetail ? { value: '' } : { amount: {} }),
        };

        onUpdateDetail(updatedDetail);
    }, [onUpdateDetail, cartDetail]);

    const onDelete = useCallback(() => {
        const filteredDetails = cartDetails.filter((_, index) => index !== cartDetailIndex);
        const updatedOrder: CheckoutOrder = {
            ...order,
            cartDetails: filteredDetails.length > 0 ? filteredDetails : undefined,
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [orders, order, cartDetails, orderIndex, cartDetailIndex, state, changeState]);

    return (
        <div className={styles.container}>
            <TextInput className={styles.label} value={label} placeholder="Label" onChange={onLabelChange} />
            <Checkbox className={styles.checkbox} checked={isPrice} onChange={onTypeChange} label="Цена" />
            <Checkbox className={styles.checkbox} checked={!isPrice} onChange={onTypeChange} label="Кастом" />
            {isPrice ?
                <Price price={value as CheckoutPrice} onChange={onPriceChange} /> :
                <TextInput
                    className={styles.custom}
                    value={value as string}
                    onChange={onValueChange}
                />
            }
            <DeleteButton onClick={onDelete} />
        </div>
    );
};

export default CartDetail;
