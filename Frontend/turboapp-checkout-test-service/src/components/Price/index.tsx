import React, { useCallback } from 'react';

import TextInput from '../TextInput';
import { CheckoutPrice } from '../../types/checkout-api';

import styles from './Price.module.css';

type PriceProps = {
    price?: CheckoutPrice;
    onChange: (price?: CheckoutPrice) => void;
};

const Price: React.FC<PriceProps> = ({ price, onChange }) => {
    const onAmountChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        onChange({
            ...price,
            value: Number(e.target.value),
        });
    }, [price, onChange]);
    const onCurrencyChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        onChange({
            ...price,
            currency: e.target.value,
        });
    }, [price, onChange]);

    return (
        <div className={styles.container}>
            <TextInput
                type="number"
                placeholder="Сумма"
                className={styles.amount}
                value={price?.value}
                onChange={onAmountChange}
            />
            <TextInput
                placeholder="Валюта"
                value={price?.currency}
                onChange={onCurrencyChange}
            />
        </div>
    );
};

export default Price;
