import React, { useCallback } from 'react';
import { cn } from '@bem-react/classname';

import CorrectIcon from '../../../../assets/tests/icons/correctIcon.svg';
import WrongIcon from '../../../../assets/tests/icons/wrongIcon.svg';

import type { IOption } from 'hooks/useTestController/types';

import './Option.scss';

export const cnTestOption = cn('TestOption');

export interface OptionProps {
    option: IOption;
    onClick: (option: IOption) => void;
    correct: boolean;
    hasResult: boolean;
    isPicked: boolean;
    answerMessage?: string | null;
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
    answerMessage,
    option,
    onClick,
    correct,
    isPicked,
    pickedText,
}) => {
    const { text } = option;
    const handleClick = useCallback(() => onClick(option), [onClick, option]);

    return (
        <div className={cnTestOption({ correct, isPicked })} onClick={handleClick}>
            <div className={cnTestOption('Text')}>{text}</div>

            {(isPicked || correct) && (
                <div className={cnTestOption('Footer')}>
                    <div className={cnTestOption('PickedComment')}>
                        {renderIcon(correct)}
                        {answerMessage}
                    </div>
                    <div className={cnTestOption('PickedText')}>
                        {isPicked &&
                            pickedText
                                ?.split('\n')
                                .map((line: string, idx: number) => (
                                    <div
                                        className={cnTestOption('Paragraph')}
                                        key={idx}
                                        dangerouslySetInnerHTML={{ __html: line }}
                                    />
                                ))}
                    </div>
                </div>
            )}
        </div>
    );
};
