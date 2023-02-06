import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../lib/array';

import { Section } from '../../../components/Section';
import { AddSection } from '../../../components/AddSection';
import { List } from '../../../components/List';
import { Presets } from '../../../components/Presets';

import { withoutTimeOption, fullDateTimeOption } from '../../../../../presets/datetime-options';
import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';
import { CheckoutDatetimeOption, CheckoutOrder } from '../../../../../types/checkout-api';

import DateOption from './DateOption';

type DateTimeOptionsProps = {
    index: number;
};

const presets = [
    { title: 'Доставка со временем', value: fullDateTimeOption },
    { title: 'Доставка без времени', value: withoutTimeOption },
];

const DateTimeOptions: React.FC<DateTimeOptionsProps> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const orders = state.orders ?? [];
    const order = orders[currentIndex] ?? {};
    const datetimeOptions = order?.datetimeOptions ?? [];

    const onAddItem = useCallback(() => {
        const updatedOrder: CheckoutOrder = {
            ...order,
            datetimeOptions: [...datetimeOptions, { id: '' }],
        };
        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [orders, order, datetimeOptions, state, changeState, currentIndex]);

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            orders: replaceItem(
                orders,
                {
                    ...order,
                    datetimeOptions: [],
                },
                currentIndex
            ),
        });
    }, [state, changeState, order, orders, currentIndex]);

    const onDelete = useCallback(() => {
        const updatedOrder: CheckoutOrder = { ...order };
        delete updatedOrder.datetimeOptions;

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [state, order, orders, changeState, currentIndex]);

    const onPresetSelect = useCallback(
        (preset: CheckoutDatetimeOption) => {
            const updatedOrder: CheckoutOrder = {
                ...order,
                datetimeOptions: [...datetimeOptions, preset],
            };
            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, currentIndex),
            });
        },
        [orders, order, datetimeOptions, state, changeState, currentIndex]
    );

    if (!('datetimeOptions' in order)) {
        return (
            <AddSection inline onClick={onAdd}>
                Дата и время доставки
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Дата и время доставки" onDelete={onDelete}>
            <List itemTitle="Дата доставки" onAdd={onAddItem}>
                {datetimeOptions.map((_, index) => (
                    <DateOption key={index} orderIndex={currentIndex} dateOptionIndex={index} />
                ))}
            </List>
            <Presets presets={presets} onSelect={onPresetSelect} />
        </Section>
    );
};

export default DateTimeOptions;
