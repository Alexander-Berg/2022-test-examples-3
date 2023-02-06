import React from 'react';
import { classnames } from '@bem-react/classnames';

import { Button } from '../../../Button';
import Checkbox from '../../../Checkbox';

import { AddButton } from '../AddButton';

import styles from './List.module.css';

type ItemProps = {
    title: string;
    className?: string;
    onDelete?: () => void;
    isSelected?: boolean;
    onSelectChange?: () => void;
};

export const ListItem: React.FC<ItemProps> = ({ className, title, children, onDelete, isSelected, onSelectChange }) => {
    return (
        <div className={classnames(styles.item, className)}>
            <div className={styles.header}>
                <label className={styles.title}>
                    {onSelectChange && (
                        <Checkbox className={styles.checkbox} checked={isSelected} onChange={onSelectChange} />
                    )}
                    {title}
                    {' #'}
                </label>

                {onDelete && (
                    <Button className={styles.delete} view="clear" size="s" onClick={onDelete}>
                        удалить
                    </Button>
                )}
            </div>
            {children}
        </div>
    );
};

type Props = {
    itemTitle: string;
    onAdd: () => void;
    className?: string;
};

export const List: React.FC<Props> = ({ children, className, itemTitle, onAdd }) => {
    return (
        <div className={classnames(styles.list, className)}>
            {children}

            <ListItem title={itemTitle}>
                <AddButton size="s" onClick={onAdd}>
                    Добавить
                </AddButton>
            </ListItem>
        </div>
    );
};
