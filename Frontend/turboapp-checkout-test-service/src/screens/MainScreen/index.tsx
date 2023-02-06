import React from 'react';

import { Button } from '../../components/Button';
import { getEventManagerUrl, getTestMerchantUrl } from '../../lib/url-builder';

import styles from './MainScreen.module.css';

const MainScreen: React.FC = () => {
    return (
        <div className={styles.container}>
            <Button className={styles.button} type="link" view="action" size="m" url={getTestMerchantUrl()}>
                Открыть тестовый магазин
            </Button>
            <Button className={styles.button} type="link" view="action" size="m" url={getEventManagerUrl()}>
                Открыть админку событий
            </Button>
        </div>
    );
};

export default MainScreen;
