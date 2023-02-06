import React, { useCallback } from 'react';
import { cn } from '@bem-react/classname';

import CorrectIcon from '../../../../assets/tests/icons/correctIcon.svg';
import WrongIcon from '../../../../assets/tests/icons/wrongIcon.svg';

import type { IOption } from 'hooks/useTestController/types';

import './Option.scss';

export const cnTestOption = cn('CategoryTestOption');

export interface OptionProps {
    option: IOption;
    onClick: (option: IOption) => void;
    hasResult: boolean;
    isPicked: boolean;
    pickedText?: string | null;
}

export const renderIcon = (isCorrect: boolean) =>
    isCorrect ? (
        // @ts-ignore
        <CorrectIcon className={cnTestOption('Icon')} />
    ) : (
        // @ts-ignore
        <WrongIcon className={cnTestOption('Icon')} />
    );

export const Option: React.FC<OptionProps> = ({
    option,
    onClick,
    isPicked,
}) => {
    const { text } = option;
    const handleClick = useCallback(() => onClick(option), [onClick, option]);

    return (
        <div className={cnTestOption({ isPicked })} onClick={handleClick}>
            {isPicked && (
                <span className={cnTestOption('PickedComment')}>
                    {renderIcon(true)}
                </span>
            )}
            <span className={cnTestOption('Text')} dangerouslySetInnerHTML={{ __html: text }} />
        </div>
    );
};
