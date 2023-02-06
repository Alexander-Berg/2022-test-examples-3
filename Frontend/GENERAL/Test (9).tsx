import React, { useCallback, useState } from 'react';
import { cn } from '@bem-react/classname';

import type { IQuestion } from 'hooks/useTestController/types';

import { urls } from '../../../urls';

import { Test } from 'components/Tests/Test/Test';
import { TestResult } from 'components/Tests/TestResult/TestResult';
import { cnTestQuestion, QuestionProps } from 'components/Tests/Question/Question';
import { cnTestOption, OptionProps, renderIcon } from 'components/Tests/Test/Option/Option';

import data from './data.json';

import './Test.scss';

const b = cn('TranslateMachineTest');

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
    image?: string;
    blankImage?: string;
}

const Question: React.FC<QuestionProps<TestQuestion>> = ({ hasResult, question }) => {
    return (
        <div className={cnTestQuestion()}>
            <div className={cnTestQuestion('Text', [b('QuestionText')])}>
                <img
                    className={b('QuestionImage')}
                    src={hasResult ? question.image : question.blankImage}
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
}) => {
    const { text } = option;
    const handleClick = useCallback(() => onClick(option), [onClick, option]);

    return (
        <div className={cnTestOption({ correct, isPicked })} onClick={handleClick}>
            <div className={cnTestOption('Text')}>{text}</div>

            {isPicked && (
                <div className={cnTestOption('Footer')}>
                    <div className={cnTestOption('PickedComment')}>
                        {renderIcon(correct)}
                        {answerMessage}
                    </div>
                    <div className={cnTestOption('PickedText')}>
                        {pickedText?.split('\n').map((line: string, idx: number) => (
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

interface Props {
    onRetry: () => void;
}

export const TranslateMachineTest: React.FC<Props> = ({ onRetry }) => {
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
                    title="Тест: Как Яндекс.Переводчик смотрит на наши тексты"
                    shortMessage={result.shortMessage}
                    message={result.message}
                    testPath={urls['machine-translation'].translate}
                    onRetryButtonClick={onRetry}
                />
            ) : (
                <Test
                    tasks={tasks}
                    onTestFinished={onTestFinished}
                    QuestionComponent={Question}
                    OptionComponent={Option}
                />
            )}
        </>
    );
};
