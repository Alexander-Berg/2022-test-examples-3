import React, { useContext } from 'react';

import { Text } from '../../../components/Text';

import { PageContext } from '../PageProvider';

import styles from './ConnectionStatus.module.css';

export const ConnectionStatus: React.FC = () => {
    const { isSocketConnected, isAdminConnected } = useContext(PageContext);

    if (isSocketConnected && isAdminConnected) {
        return null;
    }

    return (
        <div className={styles.container}>
            <Text typography="headline-m" weight="light">
                {isSocketConnected ? 'Подключение к админке…' : 'Подключение к серверу…'}
            </Text>
        </div>
    );
};
