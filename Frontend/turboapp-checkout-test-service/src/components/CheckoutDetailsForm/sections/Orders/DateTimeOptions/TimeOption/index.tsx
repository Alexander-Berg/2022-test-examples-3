import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../../lib/array';

import Checkbox from '../../../../../Checkbox';
import TextInput from '../../../../../TextInput';
import Price from '../../../../../Price';

import { DeleteButton } from '../../../../components/DeleteButton';

import { CheckoutDetailsFormContext } from '../../../../CheckoutDetailsFormProvider';
import {
    CheckoutDatetimeOption,
    CheckoutOrder,
    CheckoutTimeOption,
    CheckoutPrice,
} from '../../../../../../types/checkout-api';

type TimeOptionProps = {
    timeOptionIndex: number;
    dateOptionIndex: number;
    orderIndex: number;
};

const TimeOption: React.FC<TimeOptionProps> = ({ timeOptionIndex, dateOptionIndex, orderIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);
    const orders = state.orders ?? [];
    const order = orders[orderIndex] ?? {};
    const dateOptions = order?.datetimeOptions ?? [];
    const dateOption = dateOptions[dateOptionIndex];
    const timeOptions = dateOption?.timeOptions ?? [];
    const timeOption = timeOptions[timeOptionIndex];

    const { id, amount, label, selected } = timeOption ?? {};

    const onUpdateTime = useCallback(
        (updatedTime: CheckoutTimeOption) => {
            const updatedDateOption: CheckoutDatetimeOption = {
                ...dateOption,
                timeOptions: replaceItem(timeOptions, updatedTime, timeOptionIndex),
            };
            const updatedOrder: CheckoutOrder = {
                ...order,
                datetimeOptions: replaceItem(dateOptions, updatedDateOption, dateOptionIndex),
            };

            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, orderIndex),
            });
        },
        [
            orders,
            order,
            dateOption,
            dateOptions,
            timeOptions,
            timeOptionIndex,
            orderIndex,
            dateOptionIndex,
            state,
            changeState,
        ]
    );

    const onIdChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateTime({
                ...timeOption,
                id: e.target.value,
            });
        },
        [timeOption, onUpdateTime]
    );

    const onAmountChange = useCallback((amount?: CheckoutPrice) => {
        onUpdateTime({
            ...timeOption,
            amount,
        });
    }, [timeOption, onUpdateTime]);

    const onLabelChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateTime({
                ...timeOption,
                label: e.target.value,
                id: e.target.value,
            });
        },
        [timeOption, onUpdateTime]
    );

    const onSelectedChange = useCallback(() => {
        if (timeOption?.selected) {
            return onUpdateTime({
                ...timeOption,
                selected: undefined,
            });
        }

        const updatedDateOption: CheckoutDatetimeOption = {
            ...dateOption,
            timeOptions: timeOptions.map((option, index) => ({
                ...option,
                selected: index === timeOptionIndex ? true : undefined,
            })),
        };
        const updatedOrder: CheckoutOrder = {
            ...order,
            datetimeOptions: replaceItem(dateOptions, updatedDateOption, dateOptionIndex),
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [
        orders,
        order,
        timeOption,
        timeOptions,
        dateOption,
        dateOptions,
        orderIndex,
        dateOptionIndex,
        timeOptionIndex,
        state,
        changeState,
        onUpdateTime,
    ]);

    const onDelete = useCallback(() => {
        const updatedDateOption: CheckoutDatetimeOption = {
            ...dateOption,
            timeOptions: timeOptions.filter((_, index) => index !== timeOptionIndex),
        };
        const updatedOrder: CheckoutOrder = {
            ...order,
            datetimeOptions: replaceItem(dateOptions, updatedDateOption, dateOptionIndex),
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [
        orders,
        order,
        dateOptions,
        dateOption,
        timeOptions,
        orderIndex,
        dateOptionIndex,
        timeOptionIndex,
        state,
        changeState,
    ]);

    return (
        <>
            <Checkbox checked={Boolean(selected)} onChange={onSelectedChange} />
            <TextInput value={id} placeholder="Идентификатор" onChange={onIdChange} />
            <TextInput value={label ?? ''} placeholder="Название" onChange={onLabelChange} />
            <Price price={amount} onChange={onAmountChange} />
            <DeleteButton onClick={onDelete} />
        </>
    );
};

export default TimeOption;
