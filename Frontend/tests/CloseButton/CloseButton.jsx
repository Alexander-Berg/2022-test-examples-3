import React from 'react';
import { cn } from '@bem-react/classname';
import { Button } from '@yandex-lego/components/Button/desktop/bundle';
import './CloseButton.css';

import CloseIcon from '../../../assets/tests/icons/close.svg';

const b = cn('CloseButton');

export const CloseButton = ({
    className,
    onClick,
}) => (
    <Button
        className={ b(null, [className]) }
        aria-label="close"
        onClick={ onClick }
    >
        <CloseIcon className={ b('Icon') } />
    </Button>
);
