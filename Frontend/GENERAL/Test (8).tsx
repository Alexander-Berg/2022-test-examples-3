import React, { useCallback, useState } from 'react';
import { cn } from '@bem-react/classname';

import type { IQuestion } from 'hooks/useTestController/types';

import { Test } from 'components/Tests/Test/Test';
import { TestResult } from 'components/Tests/TestResult/TestResult';
import { cnTestOption, renderIcon, OptionProps } from 'components/Tests/Test/Option/Option';

import data from './data.json';
import { urls } from '../../../urls';
import { cnTestQuestion, QuestionProps } from 'components/Tests/Question/Question';

import './Test.scss';

const TOUCH_LAYOUT_WIDTH = 768;

const isTouchLayout = window.innerWidth <= TOUCH_LAYOUT_WIDTH;

const b = cn('ToolsOfMysteriousPurposeTest');

const getResultData = (score: number | null) => {
    let result = data.results[0];

    if (score === null) {
        return result;
    }

    for (const item of data.results) {
        if (score >= item.minScore) {
            result = item;
        }
    }

    return result;
};

const tasks = data.tasks;

interface TestQuestion extends IQuestion {
    imageTouch?: string;
    imageDesktop?: string;
    blankImage?: string;
}

const Question: React.FC<QuestionProps<TestQuestion>> = ({ question }) => {
    return (
        <div className={cnTestQuestion()}>
            <div className={cnTestQuestion('Text', [b('QuestionText')])}>
                <div
                    className={b('QuestionTitle')}
                    dangerouslySetInnerHTML={{ __html: question.text }}
                />
                <img
                    className={b('QuestionImage')}
                    src={isTouchLayout ? question.imageTouch : question.imageDesktop}
                />
            </div>
        </div>
    );
};

export const Option: React.FC<OptionProps> = ({
    answerMessage,
    option,
    onClick,
    correct,
    isPicked,
    pickedText,
    hasResult,
}) => {
    const { text } = option;
    const handleClick = useCallback(() => onClick(option), [onClick, option]);

    const displaySuggest = hasResult && (isPicked || correct);

    return (
        <div className={cnTestOption({ correct, isPicked })} onClick={handleClick}>
            <div className={cnTestOption('Text')} dangerouslySetInnerHTML={{ __html: text }} />

            {displaySuggest && (
                <div className={cnTestOption('Footer')}>
                    <div className={cnTestOption('PickedComment')}>
                        {renderIcon(correct)}
                        {answerMessage}
                    </div>
                    {isPicked && (
                        <div className={cnTestOption('PickedText')}>
                            {pickedText?.split('\n').map((line: string, idx: number) => (
                                <div
                                    className={cnTestOption('Paragraph')}
                                    key={idx}
                                    dangerouslySetInnerHTML={{ __html: line }}
                                />
                            ))}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

interface Props {
    onRetry: () => void;
}

export const ToolsOfMysteriousPurposeTest: React.FC<Props> = ({ onRetry }) => {
    const [finished, setFinished] = useState(false);
    const [score, setScore] = useState(0);

    const onTestFinished = useCallback(score => {
        setFinished(true);
        setScore(score);
    }, []);

    const result = getResultData(score);

    return (
        <>
            {finished ? (
                <TestResult
                    score={score}
                    maxScore={data.tasks.length}
                    title="Тест: взгляните на инструменты метеорологов и угадайте, зачем они нужны"
                    shortMessage={result.shortMessage}
                    message={result.message}
                    testPath={urls.weather.tools}
                    onRetryButtonClick={onRetry}
                />
            ) : (
                <Test
                    tasks={tasks}
                    onTestFinished={onTestFinished}
                    OptionComponent={Option}
                    QuestionComponent={Question}
                />
            )}
        </>
    );
};
