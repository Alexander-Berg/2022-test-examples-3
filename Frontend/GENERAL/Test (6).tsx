import React, { useCallback, useState } from 'react';

import { Test } from 'components/Tests/Test/Test';
import { TestResult } from 'components/Tests/TestResult/TestResult';

import data from './data.json';
import { urls } from '../../../urls';

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

interface Props {
    onRetry: () => void;
}

export const LingvoTest: React.FC<Props> = ({ onRetry }) => {
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
                    title="Тест: угадайте слово по родственным языкам"
                    shortMessage={result.shortMessage}
                    message={result.message}
                    testPath={urls['machine-translation'].constructor}
                    onRetryButtonClick={onRetry}
                />
            ) : (
                <Test tasks={tasks} onTestFinished={onTestFinished} />
            )}
        </>
    );
};
