import React, { useContext, useCallback } from 'react';

import { replaceItem } from '../../../../../../lib/array';

import TextInput from '../../../../../TextInput';
import { Section } from '../../../../components/Section';
import { AddSection } from '../../../../components/AddSection';

import { CheckoutDetailsFormContext } from '../../../../CheckoutDetailsFormProvider';
import { CheckoutOrderErrors } from '../../../../../../types/checkout-api';

type RequestAddressProps = {
    index: number;
};

const AddressError: React.FC<RequestAddressProps> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const orders = state.validationErrors?.orders ?? [];
    const order = orders[currentIndex] ?? {};

    const onChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const updatedOrder: CheckoutOrderErrors = {
                ...order,
                shippingAddress: {
                    address: e.target.value,
                },
            };

            changeState({
                ...state,
                validationErrors: {
                    ...state.validationErrors,
                    orders: replaceItem(orders, updatedOrder, currentIndex),
                },
            });
        },
        [state, order, orders, changeState, currentIndex]
    );

    const onAdd = useCallback(() => {
        changeState({
            ...state,
            validationErrors: {
                ...state.validationErrors,
                orders: replaceItem(
                    orders,
                    {
                        ...order,
                        shippingAddress: {
                            address: '',
                        },
                    },
                    currentIndex
                ),
            },
        });
    }, [state, changeState, order, orders, currentIndex]);

    const onDelete = useCallback(() => {
        const updatedOrder: CheckoutOrderErrors = { ...order };
        delete updatedOrder.shippingAddress;

        changeState({
            ...state,
            validationErrors: {
                ...state.validationErrors,
                orders: replaceItem(orders, updatedOrder, currentIndex),
            },
        });
    }, [state, order, orders, changeState, currentIndex]);

    if (!('shippingAddress' in order)) {
        return (
            <AddSection inline onClick={onAdd}>
                Адрес доставки
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Ошибка в адресе" onDelete={onDelete}>
            <TextInput value={order.shippingAddress?.address ?? ''} placeholder="Ошибка в адресе" onChange={onChange} />
        </Section>
    );
};

export default AddressError;
