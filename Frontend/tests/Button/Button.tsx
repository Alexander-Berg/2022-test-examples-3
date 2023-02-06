import React from 'react';
import { cn } from '@bem-react/classname';
import { Button as BaseButton } from '@yandex-lego/components/Button/desktop/bundle';
import './Button.css';

interface Props {
    className?: string;
    onClick: () => void;
    disabled?: boolean;
}

const b = cn('Button');

export const Button: React.FC<Props> = ({
    className,
    onClick,
    disabled,

    children,
}) => {
    return (
        <BaseButton
            className={b(null, [className])}
            onClick={onClick}
            disabled={disabled}
            view="default"
        >
            {children}
        </BaseButton>
    );
};
