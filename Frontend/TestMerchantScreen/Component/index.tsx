import React, { useContext, useCallback } from 'react';
// @ts-ignore
import copyToClipboard from 'copy-to-clipboard';

import CheckoutApiRequest from '../CheckoutApiRequest';
import CheckoutFrame from '../CheckoutFrame';
import { PageContext } from '../PageProvider';

import { Text } from '../../../components/Text';
import { Textinput } from '../../../components/TextInput';
import { InputLabel } from '../../../components/InputLabel';
import { Button } from '../../../components/Button';

import styles from './TestMerchantComponent.module.css';

const isJsApiAvailable = Boolean(window.YandexCheckoutRequest);

const TestMerchantComponentScreen: React.FC = () => {
    const {
        pageId,
        setPageId,
        shopName,
        setShopName,
        shopIcon,
        setShopIcon,
        baseUrl,
        setBaseUrl,
        serviceToken,
        setServiceToken,
    } = useContext(PageContext);

    const onCopy = useCallback(() => {
        copyToClipboard(pageId);
    }, [pageId]);

    const onPageIdChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        setPageId(e.target.value);
    }, [setPageId]);

    const onShopNameChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        setShopName(e.target.value || undefined);
    }, [setShopName]);

    const onShopIconChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        setShopIcon(e.target.value || undefined);
    }, [setShopIcon]);

    const onBaseUrlChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        setBaseUrl(e.target.value || undefined);
    }, [setBaseUrl]);

    const onServiceTokenChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        setServiceToken(e.target.value || undefined);
    }, [setServiceToken]);

    return (
        <div className={styles.container}>
            <Text typography="headline-l" weight="light">
                Тестовый магазин
            </Text>
            <InputLabel title="Page ID">
                <Textinput
                    className={styles.pageId}
                    view="default"
                    size="m"
                    type="text"
                    value={pageId}
                    placeholder="page id"
                    onChange={onPageIdChange}
                    iconRight={
                        <Button view="clear" size="m" onClick={onCopy}>
                            <div className={styles.copy} />
                        </Button>
                    }
                    hasClear
                />
            </InputLabel>
            <InputLabel title="Название магазина">
                <Textinput
                    view="default"
                    size="m"
                    type="text"
                    value={shopName || ''}
                    placeholder={window.location.hostname}
                    onChange={onShopNameChange}
                    hasClear
                />
            </InputLabel>
            <InputLabel title="Адрес иконки магазина">
                <Textinput
                    view="default"
                    size="m"
                    type="text"
                    value={shopIcon || ''}
                    placeholder="https://beru.ru/favicon.ico"
                    onChange={onShopIconChange}
                    hasClear
                />
            </InputLabel>
            <InputLabel title="Base Url магазина">
                <Textinput
                    view="default"
                    size="m"
                    type="text"
                    value={baseUrl || ''}
                    placeholder="/"
                    onChange={onBaseUrlChange}
                    hasClear
                />
            </InputLabel>
            <InputLabel title="Сервис токен магазина">
                <Textinput
                    view="default"
                    size="m"
                    type="text"
                    value={serviceToken || ''}
                    placeholder="service token"
                    onChange={onServiceTokenChange}
                    hasClear
                />
            </InputLabel>
            {isJsApiAvailable ? <CheckoutApiRequest /> : <CheckoutFrame />}
        </div>
    );
};

export default TestMerchantComponentScreen;
