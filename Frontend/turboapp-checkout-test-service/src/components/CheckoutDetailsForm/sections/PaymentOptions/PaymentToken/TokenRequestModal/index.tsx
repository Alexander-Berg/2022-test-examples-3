import React, { useCallback, useState } from 'react';

import { CheckoutCartItem } from '../../../../../../types/checkout-api';

import { Spin } from '../../../../../Spin';
import { Modal } from '../../../../../Modal';
import { Button } from '../../../../../Button';
import TextInput from '../../../../../TextInput';
import { InputLabel } from '../../../../../InputLabel';
import { Text } from '../../../../../Text';

import styles from './PaymentRequestModal.module.css';

type PaymentRequestBody = {
    email: string;
    goods: Array<{
        id: number;
        title: string;
        price: number;
        amount: number;
        img?: string;
    }>;
    mode?: 'test' | 'prod';
};

type Props = {
    items: CheckoutCartItem[];
    onTokenReceived: (token: string) => void;
    onClose: () => void;
};

const CROSS_ORIGIN = 'https://cors-anywhere.herokuapp.com';
const SHOP_API_ORIGIN = 'https://miniapp-external-shop-with-payments.ya-demo.ru';

export const TokenRequestModal: React.FC<Props> = ({ items, onTokenReceived, onClose }) => {
    const [email, setEmail] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setLoading] = useState(false);

    const requestToken = useCallback(() => {
        if (items.length === 0) {
            setError('Не найдены товары в заказах');
            return;
        }

        setLoading(true);
        setError('');

        const body: PaymentRequestBody = {
            email,
            goods: items.map((item, index) => {
                return {
                    id: index + 1,
                    title: item.title ?? '',
                    img: item.image?.[0]?.url,
                    price: (item.amount?.value ?? 0) / 100,
                    amount: 1,
                };
            }),
            mode: 'test',
        };
        const options: RequestInit = {
            method: 'POST',
            headers: {
                'content-type': 'application/json;charset=UTF-8',
            },
            body: JSON.stringify(body),
        };

        fetch(`${CROSS_ORIGIN}/${SHOP_API_ORIGIN}/api/cart`, options)
            .then(res => res.json())
            .then(data => data.hash)
            .then(hash => fetch(`${CROSS_ORIGIN}/${SHOP_API_ORIGIN}/api/cart/${hash}/startpaymentsviasdk`, options))
            .then(res => res.json())
            .then(data => data.payToken)
            .then(paymentToken => {
                onTokenReceived(paymentToken);
                setLoading(false);
            })
            .catch(err => {
                setError(`Не удалось получить токен: ${err}`);
                setLoading(false);
            });
    }, [email, items, onTokenReceived]);

    const onEmailChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        setEmail(e.target.value);
    }, []);

    return (
        <Modal visible theme="normal" zIndexGroupLevel={100} className={styles.modal}>
            <Text typography="headline-s" weight="light" className={styles.title}>
                Получить токен оплаты
            </Text>

            <InputLabel title="Email для чека">
                <TextInput
                    inputMode="email"
                    value={email}
                    onChange={onEmailChange}
                    placeholder="Email для чека"
                    disabled={isLoading}
                />
            </InputLabel>

            <Text typography="control-m" weight="light" className={styles.error}>
                {error}
            </Text>

            <div className={styles.buttons}>
                <Button view="action" size="s" onClick={requestToken} disabled={isLoading}>
                    Получить токен
                </Button>
                <Button view="default" size="s" onClick={onClose} disabled={isLoading}>
                    Отмена
                </Button>
            </div>

            {isLoading && <Spin view="default" size="l" progress className={styles.spin} />}
        </Modal>
    );
};
