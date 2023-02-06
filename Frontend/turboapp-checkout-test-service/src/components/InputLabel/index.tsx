import React from 'react';
import { classnames } from '@bem-react/classnames';

import { Text } from '../Text';

import styles from './InputLabel.module.css';

type Props = {
    title: string;
    className?: string;
};

export const InputLabel: React.FC<Props> = ({ title, className, children }) => {
    return (
        <label className={classnames(styles.label, className)}>
            <Text typography="control-m" weight="light" className={styles.title}>
                {title}
            </Text>
            {children}
        </label>
    );
};
