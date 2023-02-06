import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../../lib/array';

import TextInput from '../../../../../TextInput';
import { InputLabel } from '../../../../../InputLabel';

import { ListItem } from '../../../../components/List';
import { AddButton } from '../../../../components/AddButton';
import { Presets } from '../../../../components/Presets';

import { timeOption, withAmountTimeOption } from '../../../../../../presets/datetime-options';
import { CheckoutDetailsFormContext } from '../../../../CheckoutDetailsFormProvider';
import { CheckoutOrder, CheckoutDatetimeOption, CheckoutTimeOption } from '../../../../../../types/checkout-api';

import TimeOption from '../TimeOption';

import styles from './DateOption.module.css';

type DateOptionProps = {
    dateOptionIndex: number;
    orderIndex: number;
};

const presets = [
    { title: 'Бесплатная доставка', value: timeOption },
    { title: 'Платная доставка', value: withAmountTimeOption },
];

const DateOption: React.FC<DateOptionProps> = ({ orderIndex, dateOptionIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);
    const orders = state.orders ?? [];
    const order = orders[orderIndex] ?? {};
    const datetimeOptions = order?.datetimeOptions ?? [];
    const dateOption = datetimeOptions[dateOptionIndex];

    const { id, date, selected, timeOptions } = dateOption ?? {};

    const onUpdateDateOption = useCallback(
        (updatedShippingOption: CheckoutDatetimeOption) => {
            const updatedOrder: CheckoutOrder = {
                ...order,
                datetimeOptions: replaceItem(datetimeOptions, updatedShippingOption, dateOptionIndex),
            };

            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, orderIndex),
            });
        },
        [orders, order, datetimeOptions, orderIndex, dateOptionIndex, state, changeState]
    );

    const onIdChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateDateOption({
                ...dateOption,
                id: e.target.value,
            });
        },
        [dateOption, onUpdateDateOption]
    );

    const onDateChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateDateOption({
                ...dateOption,
                date: e.target.value || undefined,
                id: e.target.value,
            });
        },
        [dateOption, onUpdateDateOption]
    );

    const onSelectedChange = useCallback(() => {
        if (dateOption?.selected) {
            return onUpdateDateOption({
                ...dateOption,
                selected: undefined,
            });
        }

        const updatedOrder: CheckoutOrder = {
            ...order,
            datetimeOptions: datetimeOptions.map((option, index) => ({
                ...option,
                selected: index === dateOptionIndex ? true : undefined,
            })),
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [
        orders,
        order,
        dateOption,
        datetimeOptions,
        orderIndex,
        dateOptionIndex,
        state,
        changeState,
        onUpdateDateOption,
    ]);

    const onDelete = useCallback(() => {
        const filteredOptions = datetimeOptions.filter((_, index) => index !== dateOptionIndex);
        const updatedOrder: CheckoutOrder = {
            ...order,
            datetimeOptions: filteredOptions.length > 0 ? filteredOptions : undefined,
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [orders, order, datetimeOptions, orderIndex, dateOptionIndex, state, changeState]);

    const onTimeAdd = useCallback(() => {
        onUpdateDateOption({
            ...dateOption,
            timeOptions: [...(dateOption?.timeOptions ?? []), { id: '' }],
        });
    }, [dateOption, onUpdateDateOption]);

    const onPresetSelect = useCallback(
        (preset: CheckoutTimeOption) => {
            onUpdateDateOption({
                ...dateOption,
                timeOptions: [...(dateOption?.timeOptions ?? []), preset],
            });
        },
        [dateOption, onUpdateDateOption]
    );

    return (
        <ListItem
            title="Дата доставки"
            className={styles.option}
            onDelete={onDelete}
            isSelected={Boolean(selected)}
            onSelectChange={onSelectedChange}
        >
            <div className={styles.block}>
                <InputLabel title="Идентификатор" className={styles.id}>
                    <TextInput value={id} onChange={onIdChange} placeholder="Идентификатор" />
                </InputLabel>
                <InputLabel title="Дата">
                    <TextInput value={date} type="date" onChange={onDateChange} placeholder="Дата" />
                </InputLabel>
            </div>

            <InputLabel title="Время" />

            <div className={styles.time}>
                <InputLabel title="Идентификатор" className={styles.idTitle} />
                <InputLabel title="Название" />
                <InputLabel title="Цена" className={styles.priceTitle} />

                {timeOptions?.map((_, index) => (
                    <TimeOption
                        key={index}
                        timeOptionIndex={index}
                        dateOptionIndex={dateOptionIndex}
                        orderIndex={orderIndex}
                    />
                ))}

                <div className={styles.addButton}>
                    <AddButton size="s" onClick={onTimeAdd}>
                        Время
                    </AddButton>
                    <Presets presets={presets} onSelect={onPresetSelect} />
                </div>
            </div>
        </ListItem>
    );
};

export default DateOption;
