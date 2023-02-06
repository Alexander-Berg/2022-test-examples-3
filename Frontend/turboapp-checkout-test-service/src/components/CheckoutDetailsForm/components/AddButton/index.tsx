import React from 'react';
import { classnames } from '@bem-react/classnames';

import { Button, ButtonProps } from '../../../Button';
import { Icon } from '../../../Icon';

import styles from './AddButton.module.css';

type Props = {
    onClick: () => void;
    size?: ButtonProps['size'];
};

function CrossIcon(className: string) {
    return <Icon glyph="type-cross" size="m" className={classnames(className, styles.icon)} />;
}

export const AddButton: React.FC<Props> = ({ size = 'm', onClick, children }) => {
    return (
        <Button view="pseudo" size={size} onClick={onClick} iconLeft={CrossIcon}>
            {children}
        </Button>
    );
};
