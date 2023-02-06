import React, { useCallback, useContext } from 'react';

import Price from '../../../Price';
import { AddButton } from '../../components/AddButton';
import { Section } from '../../components/Section';
import { AddSection } from '../../components/AddSection';
import { Presets } from '../../components/Presets';

import { CheckoutDetailsFormContext } from '../../CheckoutDetailsFormProvider';
import {
    CheckoutTotalAmount,
    CheckoutTotalAmountDetail,
    Currency,
    CheckoutPrice
} from '../../../../types/checkout-api';

import { TotalDetails } from './TotalDetails';

const amountPresets = [
    { title: '1.000р.', value: 1000 * 100 },
    { title: '1.000,90р.', value: 1000 * 100 + 90 },
    { title: '1.000,99р.', value: 1000 * 100 + 99 },
    { title: '15.990р.', value: 15990 * 100 },
    { title: '1.000.000р.', value: 1000 * 1000 * 100 },
];

const detailsPresets = [
    { title: 'Товары', value: { label: 'Товар', amount: { value: 15990 * 100, currency: Currency.Rub } } },
    { title: 'Доставка', value: { label: 'Доставка', amount: { value: 500 * 100, currency: Currency.Rub } } },
    { title: 'Скидка', value: { label: 'Скидка MasterCard', amount: { value: -500 * 100, currency: Currency.Rub } } },
    {
        title: 'Промокод',
        value: { label: 'Скидка по промокоду', amount: { value: -200 * 100, currency: Currency.Rub } },
    },
];

const Total: React.FC = () => {
    const { defaultState, state, changeState } = useContext(CheckoutDetailsFormContext);

    const total = state.total ?? {};
    const details = state.total?.details || [];

    const onAmountChange = useCallback((amount?: CheckoutPrice) => {
        changeState({
            ...state,
            total: {
                ...state.total,
                amount,
            },
        });
    }, [state, changeState]);
    const onDeleteAmount = useCallback(() => {
        const newTotal: CheckoutTotalAmount = { ...state.total };
        delete newTotal.amount;

        changeState({
            ...state,
            total: newTotal,
        });
    }, [state, changeState]);

    const onAddDetailsItem = useCallback(() => {
        changeState({
            ...state,
            total: {
                ...state.total,
                details: [
                    ...(state.total?.details ?? []),
                    {
                        label: '',
                        amount: {
                            value: 0,
                            currency: Currency.Rub,
                        },
                    },
                ],
            },
        });
    }, [state, changeState]);

    const onDeleteDetails = useCallback(() => {
        const newTotal: CheckoutTotalAmount = { ...state.total };
        delete newTotal.details;

        changeState({
            ...state,
            total: newTotal,
        });
    }, [state, changeState]);

    const onAdd = useCallback(() => {
        changeState({ ...state, total: defaultState.total ?? {} });
    }, [defaultState, state, changeState]);

    const onDelete = useCallback(() => {
        const newState = { ...state };
        delete newState.total;

        changeState(newState);
    }, [state, changeState]);

    const onUpdateTotal = useCallback(
        (updatedTotal: CheckoutTotalAmount) => {
            changeState({
                ...state,
                total: {
                    ...state.total,
                    ...updatedTotal,
                },
            });
        },
        [state, changeState]
    );

    const onAddAmount = useCallback(() => {
        onUpdateTotal({ amount: { value: 0, currency: Currency.Rub } });
    }, [onUpdateTotal]);

    const onAddDetails = useCallback(() => {
        onUpdateTotal({
            details: [],
        });
    }, [onUpdateTotal]);

    const onAmountPresetSelect = useCallback(
        (preset: number) => {
            changeState({
                ...state,
                total: {
                    ...state.total,
                    amount: {
                        value: preset,
                        currency: Currency.Rub,
                    },
                },
            });
        },
        [state, changeState]
    );

    const onDetailsPresetSelect = useCallback(
        (preset: CheckoutTotalAmountDetail) => {
            changeState({
                ...state,
                total: {
                    ...state.total,
                    details: [...(state.total?.details ?? []), preset],
                },
            });
        },
        [state, changeState]
    );

    if (!('total' in state)) {
        return <AddSection onClick={onAdd}>Итоговая стоимость</AddSection>;
    }

    return (
        <Section title="Итоговая стоимость" onDelete={onDelete}>
            {'amount' in total ? (
                <Section isSubSection title="Цена" onDelete={onDeleteAmount}>
                    <Price
                        price={total.amount}
                        onChange={onAmountChange}
                    />
                    <Presets presets={amountPresets} onSelect={onAmountPresetSelect} />
                </Section>
            ) : (
                <AddSection inline onClick={onAddAmount}>
                    Цена
                </AddSection>
            )}
            {'details' in total ? (
                <Section isSubSection title="Детализация" onDelete={onDeleteDetails}>
                    {details.map((_, index) => (
                        <TotalDetails key={index} index={index} />
                    ))}
                    <AddButton size="s" onClick={onAddDetailsItem}>
                        Добавить
                    </AddButton>
                    <Presets presets={detailsPresets} onSelect={onDetailsPresetSelect} />
                </Section>
            ) : (
                <AddSection inline onClick={onAddDetails}>
                    Детализация
                </AddSection>
            )}
        </Section>
    );
};

export default Total;
