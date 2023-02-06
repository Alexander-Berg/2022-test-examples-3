import React from 'react';
import { cn } from '@bem-react/classname';

import { Shares } from 'components/Shares/Shares';
import { Button } from '../Button/Button';

import './TestResult.css';

interface Props {
    title: string;
    testPath: string;
    onRetryButtonClick: () => void;
    isTransparent?: boolean;
    topImage?: string;
    shortMessage?: string;
    message?: string;
    score?: number;
    maxScore?: number;
}

const b = cn('TestResult');

export const TestResult: React.FC<Props> = ({
    score,
    maxScore,
    title,
    message = '',
    isTransparent,
    topImage,
    shortMessage,
    onRetryButtonClick,
    testPath,
}) => {
    const scoreParam = score || 0;

    return (
        <div className={b({ transparent: isTransparent })}>
            <div className={b('Content')}>
                <div className={b('Title', { noMargin: Boolean(topImage) })}>{title}</div>

                {score && (
                    <div className={b('Score')}>
                        {(
                            maxScore ?
                                `${score} / ${maxScore}` :
                                score
                        )}
                    </div>
                )}

                {topImage && (
                    <img
                        className={b('TopImage')}
                        src={topImage}
                    />
                )}

                {shortMessage && (
                    <div
                        className={b('ShortMessage')}
                        dangerouslySetInnerHTML={{ __html: shortMessage }}
                    />
                )}

                <div
                    className={b('Message')}
                    dangerouslySetInnerHTML={{ __html: message + '<br />' }}
                />

                <div className={b('Social')}>
                    Поделиться
                    <Shares
                        customUrl={`https://techno.yandex.ru${testPath}?res=${scoreParam}`}
                        className={b('shares')}
                        goalType={`test.result.${testPath}`}
                    />
                </div>

                <div className={b('Controls')}>
                    <Button className={b('RestartButton')} onClick={onRetryButtonClick}>
                        Пройти ещё раз
                    </Button>
                </div>
            </div>
        </div>
    );
};
