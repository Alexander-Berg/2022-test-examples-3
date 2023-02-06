import React from 'react';
import { cn } from '@bem-react/classname';

import { Button } from '../Button/Button';

import './Intro.css';

interface Props {
    text?: string;
    buttonText?: string;
    heading: string;
    headingBold: string;
    onStart: () => void;
}

const b = cn('Intro');

export const Intro: React.FC<Props> = ({
    text,
    buttonText = 'Начать игру',
    heading,
    headingBold,
    children,

    onStart,
}) => {
    return (
        <div className={b()}>
            <div className={b('Content')}>
                <div className={b('Heading')}>
                    <span className={b('Bold')}>{headingBold} </span>
                    <span>{heading}</span>
                </div>

                <div className={b('Text')}>
                    {text &&
                        text
                            .split('\n')
                            .map((line, idx) => (
                                <p
                                    className={b('Paragraph')}
                                    key={idx}
                                    dangerouslySetInnerHTML={{ __html: line }}
                                />
                            ))}
                    {children}
                </div>
            </div>

            <Button className={b('StartButton')} onClick={onStart}>
                {buttonText}
            </Button>
        </div>
    );
};
