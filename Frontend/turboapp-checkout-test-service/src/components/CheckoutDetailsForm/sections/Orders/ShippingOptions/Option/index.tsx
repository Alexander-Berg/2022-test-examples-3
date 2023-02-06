import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../../lib/array';

import TextInput from '../../../../../TextInput';
import { InputLabel } from '../../../../../InputLabel';
import Price from '../../../../../Price';

import { ListItem } from '../../../../components/List';

import { CheckoutDetailsFormContext } from '../../../../CheckoutDetailsFormProvider';
import { CheckoutShippingOption, CheckoutOrder, CheckoutPrice } from '../../../../../../types/checkout-api';

import styles from './ShippingOption.module.css';

type ShippingOptionProps = {
    shippingOptionIndex: number;
    orderIndex: number;
};

const ShippingOption: React.FC<ShippingOptionProps> = ({ orderIndex, shippingOptionIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);
    const orders = state.orders ?? [];
    const order = orders[orderIndex];
    const shippingOptions = order?.shippingOptions ?? [];
    const shippingOption = shippingOptions[shippingOptionIndex];

    const { id, amount, label, datetimeEstimate, selected } = shippingOption ?? {};

    const onUpdateShippingOption = useCallback(
        (updatedShippingOption: CheckoutShippingOption) => {
            const updatedOrder: CheckoutOrder = {
                ...order,
                shippingOptions: replaceItem(shippingOptions, updatedShippingOption, shippingOptionIndex),
            };

            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, orderIndex),
            });
        },
        [orders, order, shippingOptions, orderIndex, shippingOptionIndex, state, changeState]
    );

    const onIdChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateShippingOption({
                ...shippingOption,
                id: e.target.value,
            });
        },
        [shippingOption, onUpdateShippingOption]
    );

    const onLabelChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateShippingOption({
                ...shippingOption,
                label: e.target.value || undefined,
            });
        },
        [shippingOption, onUpdateShippingOption]
    );

    const onDatetimeEstimateChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateShippingOption({
                ...shippingOption,
                datetimeEstimate: e.target.value || undefined,
            });
        },
        [shippingOption, onUpdateShippingOption]
    );

    const onAmountChange = useCallback((amount?: CheckoutPrice) => {
        onUpdateShippingOption({
            ...shippingOption,
            amount,
        });
    }, [shippingOption, onUpdateShippingOption]);

    const onSelectedChange = useCallback(() => {
        if (shippingOption?.selected) {
            return onUpdateShippingOption({
                ...shippingOption,
                selected: undefined,
            });
        }

        const updatedOrder: CheckoutOrder = {
            ...order,
            shippingOptions: shippingOptions.map((option, index) => ({
                ...option,
                selected: index === shippingOptionIndex ? true : undefined,
            })),
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [
        orders,
        order,
        shippingOption,
        shippingOptions,
        orderIndex,
        shippingOptionIndex,
        state,
        changeState,
        onUpdateShippingOption,
    ]);

    const onDelete = useCallback(() => {
        const filteredOptions = shippingOptions.filter((_, index) => index !== shippingOptionIndex);
        const updatedOrder: CheckoutOrder = {
            ...order,
            shippingOptions: filteredOptions.length > 0 ? filteredOptions : undefined,
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [orders, order, shippingOptions, orderIndex, shippingOptionIndex, state, changeState]);

    return (
        <ListItem
            className={styles.item}
            title="Способ доставки"
            onDelete={onDelete}
            isSelected={Boolean(selected)}
            onSelectChange={onSelectedChange}
        >
            <InputLabel title="Идентификатор">
                <TextInput value={id} onChange={onIdChange} placeholder="Идентификатор" />
            </InputLabel>
            <InputLabel title="Цена">
                <Price price={amount} onChange={onAmountChange} />
            </InputLabel>
            <InputLabel title="Название">
                <TextInput value={label} onChange={onLabelChange} placeholder="Название" />
            </InputLabel>
            <InputLabel title="Время доставки">
                <TextInput value={datetimeEstimate} onChange={onDatetimeEstimateChange} placeholder="Время доставки" />
            </InputLabel>
        </ListItem>
    );
};

export default ShippingOption;
