import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../lib/array';

import { Section } from '../../../components/Section';
import { AddSection } from '../../../components/AddSection';
import { List } from '../../../components/List';
import { Presets } from '../../../components/Presets';

import {
    uniqPickupOptions,
    uniqPickupOptionsWithoutCoordinates,
    generatePickupOptions,
} from '../../../../../presets/pickup-options';
import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';
import { CheckoutOrder, CheckoutPickupOption } from '../../../../../types/checkout-api';

import PickupOption from './Option';

type PickupOptionsProps = {
    index: number;
};

enum PresetType {
    Uniq,
    UniqWithoutCoordinates,
    Random,
}

const listPresets = [
    { title: 'Точки самовывоза с координатами', value: PresetType.Uniq },
    { title: 'Точки самовывоза без координат', value: PresetType.UniqWithoutCoordinates },
    { title: 'Сгенерировать точки самовывоза', value: PresetType.Random }
];

const itemPresets = [
    { title: 'Точка самовывоза', value: uniqPickupOptions[0] },
];

const PickupOptions: React.FC<PickupOptionsProps> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const orders = state.orders ?? [];
    const order = orders[currentIndex] ?? {};
    const pickupOptions = order?.pickupOptions ?? [];

    const onAddItem = useCallback(() => {
        const updatedOrder: CheckoutOrder = {
            ...order,
            pickupOptions: [...pickupOptions, { id: `pickup-${pickupOptions.length + 1}` }],
        };
        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [orders, order, pickupOptions, state, changeState, currentIndex]);

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            orders: replaceItem(
                orders,
                {
                    ...order,
                    pickupOptions: [],
                },
                currentIndex
            ),
        });
    }, [state, changeState, order, orders, currentIndex]);

    const onDelete = useCallback(() => {
        const updatedOrder: CheckoutOrder = { ...order };
        delete updatedOrder.pickupOptions;

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [state, order, orders, changeState, currentIndex]);

    const onPresetSelect = useCallback(
        (preset: PresetType) => {
            let pickupOptions: CheckoutPickupOption[];
            switch (preset) {
                case PresetType.Uniq:
                    pickupOptions = uniqPickupOptions;
                    break;
                case PresetType.UniqWithoutCoordinates:
                    pickupOptions = uniqPickupOptionsWithoutCoordinates;
                    break;
                case PresetType.Random:
                    const count = parseInt(
                        window.prompt(
                            'Введите количество точек самовывоза',
                            '5'
                        ) || '5'
                    ) || 5;
                    pickupOptions = generatePickupOptions(count);
            }

            const updatedOrder: CheckoutOrder = {
                ...order,
                pickupOptions,
            };
            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, currentIndex),
            });
        },
        [orders, order, state, changeState, currentIndex]
    );

    const onPickupOptionPresetSelect = useCallback((preset: CheckoutPickupOption) => {
        const updatedOrder: CheckoutOrder = {
            ...order,
            pickupOptions: [...pickupOptions, preset],
        };
        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, currentIndex),
        });
    }, [orders, order, pickupOptions, state, changeState, currentIndex]);

    if (!('pickupOptions' in order)) {
        return (
            <AddSection inline onClick={onAdd}>
                Точки самовывоза
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Точки самовывоза" onDelete={onDelete}>
            <Presets presets={listPresets} onSelect={onPresetSelect} />
            <List itemTitle="Точка самовывоза" onAdd={onAddItem}>
                {pickupOptions.map((pickupOption, index) => (
                    <PickupOption key={pickupOption?.id ?? index} orderIndex={currentIndex} pickupOptionIndex={index} />
                ))}
            </List>
            <Presets presets={itemPresets} onSelect={onPickupOptionPresetSelect} />
        </Section>
    );
};

export default PickupOptions;
