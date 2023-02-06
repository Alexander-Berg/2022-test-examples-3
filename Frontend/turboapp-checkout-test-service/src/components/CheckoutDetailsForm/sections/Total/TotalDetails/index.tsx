import React, { useCallback, useContext } from 'react';

import { replaceItem } from '../../../../../lib/array';
import { CheckoutTotalAmountDetail, CheckoutPrice } from '../../../../../types/checkout-api';

import TextInput from '../../../../TextInput';
import { InputLabel } from '../../../../InputLabel';
import Price from '../../../../Price';

import { DeleteButton } from '../../../components/DeleteButton';
import { CheckoutDetailsFormContext } from '../../../CheckoutDetailsFormProvider';

import styles from './TotalDetails.module.css';

export const TotalDetails: React.FC<{ index: number }> = ({ index: currentIndex }) => {
    const { state, changeState } = useContext(CheckoutDetailsFormContext);

    const details = state.total?.details ?? [];
    const detail = details[currentIndex] ?? {};

    const { label, amount } = detail;

    const onUpdateDetail = useCallback(
        (updatedDetails: CheckoutTotalAmountDetail) => {
            changeState({
                ...state,
                total: {
                    ...state.total,
                    details: replaceItem(details, updatedDetails, currentIndex),
                },
            });
        },
        [state, details, currentIndex, changeState]
    );

    const onLabelChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            onUpdateDetail({
                ...detail,
                label: e.target.value,
            });
        },
        [detail, onUpdateDetail]
    );

    const onAmountChange = useCallback((amount?: CheckoutPrice) => {
        onUpdateDetail({
            ...detail,
            amount
        });
    }, [detail, onUpdateDetail]);

    const onDelete = useCallback(() => {
        changeState({
            ...state,
            total: {
                ...state.total,
                details: details.filter((_, index) => index !== currentIndex),
            },
        });
    }, [state, details, changeState, currentIndex]);

    return (
        <div className={styles.detail}>
            <InputLabel title="Текст" className={styles.label}>
                <TextInput className={styles.label} value={label} onChange={onLabelChange} placeholder="Текст" />
            </InputLabel>
            <InputLabel title="Цена" className={styles.amount}>
                <Price price={amount} onChange={onAmountChange} />
            </InputLabel>
            <DeleteButton onClick={onDelete} className={styles.delete} />
        </div>
    );
};
