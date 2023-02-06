import React, { useState, useCallback } from 'react';
import { classnames } from '@bem-react/classnames';

import { Button } from '../../components/Button';
import { Textinput } from '../../components/TextInput';
import { InputLabel } from '../../components/InputLabel';

import styles from './TestPaymentFormScreen.module.css';

const checkoutCallbacksMap = {
    success: '/online-external-payments/success',
    failed: '/online-external-payments/failed',
    cancel: '/online-external-payments/cancel',
};

const TestPaymentFormScreen: React.FC = () => {
    const [checkoutOrigin, setCheckoutOrigin] = useState(() => {
        if (document.referrer) {
            return new URL(document.referrer).origin;
        }

        return 'https://checkout.tap-tst.yandex.ru';
    });

    const onRedirect = useCallback(
        (checkoutPath: string) => {
            const redirectUrl = `${checkoutOrigin}${checkoutPath}`;

            window.location.href = redirectUrl;
        },
        [checkoutOrigin]
    );

    const onSuccess = useCallback(() => onRedirect(checkoutCallbacksMap.success), [onRedirect]);
    const onFailed = useCallback(() => onRedirect(checkoutCallbacksMap.failed), [onRedirect]);
    const onCancel = useCallback(() => onRedirect(checkoutCallbacksMap.cancel), [onRedirect]);

    const onCheckoutOriginChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        setCheckoutOrigin(e.target.value);
    }, []);

    return (
        <div className={styles.container}>
            <InputLabel className={styles.input} title="Адрес чекаута">
                <Textinput
                    view="default"
                    size="m"
                    type="url"
                    autoComplete="on"
                    value={checkoutOrigin}
                    placeholder="Адрес чекаута"
                    onChange={onCheckoutOriginChange}
                />
            </InputLabel>
            <Button className={classnames(styles.button, styles.success)} view="action" size="m" onClick={onSuccess}>
                Успешная оплата
            </Button>
            <Button className={classnames(styles.button, styles.failed)} view="action" size="m" onClick={onFailed}>
                Неудачная оплата
            </Button>
            <Button className={classnames(styles.button, styles.cancel)} view="action" size="m" onClick={onCancel}>
                Отмена оплаты
            </Button>
        </div>
    );
};

export default TestPaymentFormScreen;
