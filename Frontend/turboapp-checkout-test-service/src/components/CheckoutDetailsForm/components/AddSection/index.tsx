import React from 'react';
import { classnames } from '@bem-react/classnames';

import { Button } from '../../../Button';
import { Icon } from '../../../Icon';

import styles from './AddSection.module.css';

type Props = {
    onClick: () => void;
    inline?: boolean;
    className?: string;
};

export const AddSection: React.FC<Props> = ({ onClick, className, inline, children }) => {
    return (
        <div className={classnames(styles.section, className, inline ? styles.inline : undefined)}>
            <Button className={styles.button} view="default" size="m" width={inline ? 'auto' : 'max'} onClick={onClick}>
                <Icon glyph="type-cross" size="m" className={styles.icon} /> {children}
            </Button>
        </div>
    );
};
