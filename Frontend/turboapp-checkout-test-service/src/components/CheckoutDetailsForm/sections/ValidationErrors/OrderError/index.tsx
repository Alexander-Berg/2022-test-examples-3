import React, { useContext, useCallback, useMemo, useState } from 'react';
import { CheckoutValidationErrors } from '../../../../../types/checkout-api';

import { Section } from '../../../components/Section';
import { AddSection } from '../../../components/AddSection';
import { TabsSection } from '../../../components/TabsSection';
import { AddButton } from '../../../components/AddButton';

import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';

import Id from './Id';
import AddressError from './AddressError';
import CityError from './CityError';

const OrderErrors: React.FC = () => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const validationErrors = state.validationErrors ?? {};
    const orders = validationErrors.orders ?? [];

    const [activeTab, setActiveTab] = useState('0');
    const maxIndex = (state.validationErrors?.orders?.length ?? 0) - 1;
    const index = Math.min(Number(activeTab), maxIndex);

    const onAdd = useCallback(() => {
        const id = state.orders?.[0]?.id || 'order-1';

        changeState({
            ...state,
            validationErrors: {
                ...validationErrors,
                orders: [{ id }],
            },
        });
    }, [state, validationErrors, changeState]);

    const onDelete = useCallback(() => {
        const validationErrors: CheckoutValidationErrors = { ...state.validationErrors };
        delete validationErrors.orders;

        changeState({
            ...state,
            validationErrors,
        });
    }, [state, changeState]);

    const onOrderAdd = useCallback(() => {
        changeState({
            ...state,
            validationErrors: {
                ...state.validationErrors,
                orders: state.validationErrors?.orders?.concat({
                    id: `order-${state.validationErrors?.orders?.length + 1}`,
                    shippingAddress: { address: '' },
                }),
            },
        });

        setActiveTab(String(state.validationErrors?.orders?.length));
    }, [state, changeState]);

    const onOrderDelete = useCallback(
        (index: number) => {
            changeState({
                ...state,
                validationErrors: {
                    ...state.validationErrors,
                    orders: [...orders.slice(0, index), ...orders.slice(index + 1)],
                },
            });
        },
        [orders, changeState]
    );

    const tabs = useMemo(() => {
        return Array(orders?.length || 0)
            .fill(0)
            .map((_, index) => ({
                id: String(index),
                title: `Заказ #${index + 1}`,
                onClose: () => onOrderDelete(index),
            }));
    }, [orders, state, onOrderDelete]);

    if (!('orders' in validationErrors)) {
        return (
            <AddSection inline onClick={onAdd}>
                Ошибки в заказах
            </AddSection>
        );
    }

    return (
        <Section isSubSection title="Ошибки в заказах" onDelete={onDelete}>
            {index === -1 && (
                <AddButton size="s" onClick={onOrderAdd}>
                    Заказ
                </AddButton>
            )}
            {index !== -1 && (
                <>
                    <TabsSection tabs={tabs} activeTab={String(index)} onChange={setActiveTab} onAdd={onOrderAdd} />

                    <Id index={index} />
                    <AddressError index={index} />
                    <CityError index={index} />
                </>
            )}
        </Section>
    );
};

export default OrderErrors;
