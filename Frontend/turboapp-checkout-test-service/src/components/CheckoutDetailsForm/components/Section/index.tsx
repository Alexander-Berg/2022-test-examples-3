import React, { useCallback, useState } from 'react';
import { classnames } from '@bem-react/classnames';

import { Text } from '../../../Text';
import { Button } from '../../../Button';
import { Icon } from '../../../Icon';

import styles from './Section.module.css';

type Props = {
    title: string;
    onDelete: () => void;
    className?: string;
    isSubSection?: boolean;
};

export const Section: React.FC<Props> = ({ title, onDelete, className, isSubSection, children }) => {
    const [collapsed, setCollapsed] = useState(false);

    const onTitleClick = useCallback(() => {
        setCollapsed(state => !state);
    }, []);

    return (
        <div className={classnames(styles.section, className, isSubSection ? styles.subSection : undefined)}>
            <div className={styles.header}>
                <div onClick={onTitleClick} className={styles.title}>
                    <Icon type="arrow" direction={collapsed ? 'right' : 'bottom'} className={styles.icon} />
                    <Text typography={isSubSection ? 'headline-s' : 'headline-m'} weight="light">
                        {title}
                    </Text>
                </div>
                <Button className={styles.delete} view="clear" size="s" onClick={onDelete}>
                    удалить
                </Button>
            </div>
            {!collapsed && children}
        </div>
    );
};
