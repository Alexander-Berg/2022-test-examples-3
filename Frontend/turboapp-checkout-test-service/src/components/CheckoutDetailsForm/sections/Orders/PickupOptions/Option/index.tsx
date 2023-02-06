import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../../lib/array';

import TextInput from '../../../../../TextInput';
import { InputLabel } from '../../../../../InputLabel';

import { ListItem } from '../../../../components/List';

import { CheckoutDetailsFormContext } from '../../../../CheckoutDetailsFormProvider';
import { CheckoutPickupOption, CheckoutOrder } from '../../../../../../types/checkout-api';

import styles from './PickupOption.module.css';

type PickupOptionProps = {
    pickupOptionIndex: number;
    orderIndex: number;
};

const PickupOption: React.FC<PickupOptionProps> = ({ orderIndex, pickupOptionIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);
    const orders = state.orders ?? [];
    const order = orders[orderIndex];
    const pickupOptions = order?.pickupOptions ?? [];
    const pickupOption = pickupOptions[pickupOptionIndex];

    const { id, label, address, selected, coordinates = {} } = pickupOption ?? {};
    const { lon, lat } = coordinates;

    const onUpdatePickupOption = useCallback(
        (updatedPickupOption: CheckoutPickupOption) => {
            const updatedOrder: CheckoutOrder = {
                ...order,
                pickupOptions: replaceItem(pickupOptions, updatedPickupOption, pickupOptionIndex),
            };

            changeState({
                ...state,
                orders: replaceItem(orders, updatedOrder, orderIndex),
            });
        },
        [orders, order, pickupOptions, orderIndex, pickupOptionIndex, state, changeState]
    );

    const onIdChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdatePickupOption({
                ...pickupOption,
                id: e.target.value,
            });
        },
        [pickupOption, onUpdatePickupOption]
    );

    const onLabelChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdatePickupOption({
                ...pickupOption,
                label: e.target.value || undefined,
            });
        },
        [pickupOption, onUpdatePickupOption]
    );

    const onAddressChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdatePickupOption({
                ...pickupOption,
                address: e.target.value || undefined,
            });
        },
        [pickupOption, onUpdatePickupOption]
    );

    const onLatChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const coordinates = {
                ...pickupOption?.coordinates,
                lat: Number(e.target.value) || undefined,
            };
            onUpdatePickupOption({
                ...pickupOption,
                coordinates,
            });
        },
        [pickupOption, onUpdatePickupOption]
    );

    const onLonChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const coordinates = {
                ...pickupOption?.coordinates,
                lon: Number(e.target.value) || undefined,
            };
            onUpdatePickupOption({
                ...pickupOption,
                coordinates,
            });
        },
        [pickupOption, onUpdatePickupOption]
    );

    const onSelectedChange = useCallback(() => {
        if (pickupOption?.selected) {
            return onUpdatePickupOption({
                ...pickupOption,
                selected: undefined,
            });
        }

        const updatedOrder: CheckoutOrder = {
            ...order,
            pickupOptions: pickupOptions.map((option, index) => ({
                ...option,
                selected: index === pickupOptionIndex ? true : undefined,
            })),
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [
        orders,
        order,
        pickupOption,
        pickupOptions,
        orderIndex,
        pickupOptionIndex,
        state,
        changeState,
        onUpdatePickupOption,
    ]);

    const onDelete = useCallback(() => {
        const filteredOptions = pickupOptions.filter((_, index) => index !== pickupOptionIndex);
        const updatedOrder: CheckoutOrder = {
            ...order,
            pickupOptions: filteredOptions.length > 0 ? filteredOptions : undefined,
        };

        changeState({
            ...state,
            orders: replaceItem(orders, updatedOrder, orderIndex),
        });
    }, [orders, order, pickupOptions, orderIndex, pickupOptionIndex, state, changeState]);

    return (
        <ListItem
            className={styles.item}
            title="Точка самовывоза"
            onDelete={onDelete}
            isSelected={Boolean(selected)}
            onSelectChange={onSelectedChange}
        >
            <InputLabel title="Идентификатор">
                <TextInput value={id} onChange={onIdChange} placeholder="Идентификатор" />
            </InputLabel>
            <InputLabel title="Название">
                <TextInput value={label} onChange={onLabelChange} placeholder="Название" />
            </InputLabel>
            <InputLabel title="Aдрес" className={styles.address}>
                <TextInput value={address} onChange={onAddressChange} placeholder="Адрес" />
            </InputLabel>
            <InputLabel title="Координаты" className={styles.coords}>
                <div className={styles['coords-container']}>
                    <TextInput value={lat} type="number" onChange={onLatChange} placeholder="Широта" />
                    <TextInput value={lon} type="number" onChange={onLonChange} placeholder="Долгота" />
                </div>
            </InputLabel>
        </ListItem>
    );
};

export default PickupOption;
