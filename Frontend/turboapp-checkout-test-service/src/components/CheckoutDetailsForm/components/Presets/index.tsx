import React, { useMemo } from 'react';
import { classnames } from '@bem-react/classnames';

import { Link } from '../../../Link';
import { Text } from '../../../Text';

import styles from './Presets.module.css';

export type Preset<T> = {
    title: string;
    value: T;
};

type Props<T> = {
    presets: Preset<T>[];
    onSelect: (value: T) => void;
    className?: string;
};

export function Presets<T>({ presets, onSelect, className }: Props<T>) {
    const list = useMemo(() => {
        return presets.map((preset, index) => {
            const onClick = () => onSelect(preset.value);
            return (
                <>
                    <Link key={index} theme="pseudo" onClick={onClick} className={styles.preset}>
                        {preset.title}
                    </Link>
                </>
            );
        });
    }, [presets, onSelect]);

    return (
        <Text typography="control-m" weight="light" className={classnames(styles.block, className)}>
            Пресеты:
            {list}
        </Text>
    );
}
