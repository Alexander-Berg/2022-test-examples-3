import React, { useCallback, useState } from 'react';

import { Test } from 'components/Tests/Test/Test';
import { TestResult } from 'components/Tests/TestResult/TestResult';
import { cnTestOption, renderIcon, OptionProps } from 'components/Tests/Test/Option/Option';
import type { CheckAnswerFn } from 'hooks/useTestController/types';

import data from './data.json';
import { urls } from '../../../urls';

export const checkAnswer: CheckAnswerFn = (option, task) => {
    const correctAnswer = task.answer === 'any' ? option.key : task.answer;

    const result = {
        isCorrect: correctAnswer === option.key,
        correctAnswer,
    };

    return Promise.resolve(result);
};

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

const Option: React.FC<OptionProps> = ({
    option,
    onClick,
    correct,
    isPicked,
    pickedText,
    answerMessage,
}) => {
    const { text } = option;
    const handleClick = useCallback(() => onClick(option), [onClick, option]);

    return (
        <div className={cnTestOption({ correct, isPicked })} onClick={handleClick}>
            <div
                className={cnTestOption('Text')}
                dangerouslySetInnerHTML={{ __html: text }}
            />

            {isPicked && (
                <div className={cnTestOption('Footer')}>
                    <div className={cnTestOption('PickedComment')}>
                        {renderIcon(correct)}
                        {answerMessage}
                    </div>
                    <div className={cnTestOption('PickedText')}>
                        {pickedText?.split('\n').map((line, i) => (
                            <div key={i} dangerouslySetInnerHTML={{ __html: line }} />
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

export const DrivingLicenseTest: React.FC<Props> = ({ onRetry }) => {
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
                    title="Сдаём на права"
                    shortMessage={result.shortMessage}
                    message={result.message}
                    testPath={urls['self-driving'].theory}
                    onRetryButtonClick={onRetry}
                />
            ) : (
                <Test
                    tasks={tasks}
                    onTestFinished={onTestFinished}
                    checkAnswer={checkAnswer}
                    OptionComponent={Option}
                />
            )}
        </>
    );
};
