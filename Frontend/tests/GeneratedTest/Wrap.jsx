import React, { useCallback, useState, useMemo } from 'react';

import data from './data.json';

import { GeneratedTest } from './GeneratedTest';
import { TestResult } from '../TestResult/TestResult';
import { urls } from '../../../../urls';

const checkAnswer = (option, _task, question) => {
    return Promise.resolve({
        isCorrect: question.source === option.key,
        correctAnswer: question.source,
    });
};

const getResultData = score => {
    let result = {};

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

const getRandomizedTasks = () => data.quotes.map(item => {
    const source = Math.random() >= 0.5 ? 'authentic' : 'generated';

    const idx = Math.floor(Math.random() * item[source].length);
    const quote = item[source][idx];

    return {
        question: {
            text: 'Попробуйте отличить великого классика от алгоритма по цитате:',
            quote,
            source,
        },

        options: [
            {
                text: item.author,
                key: 'authentic',
            },
            {
                text: 'Алгоритм',
                key: 'generated',
            },
        ],
    };
});

export const GeneratedTestWrap = ({
    onRetry,
}) => {
    const [finished, setFinished] = useState(false);
    const [score, setScore] = useState(null);

    const onTestFinished = useCallback(score => {
        setFinished(true);
        setScore(score);
    }, []);

    const tasks = useMemo(getRandomizedTasks, []);
    const result = getResultData(score);

    return (
        <>
            { finished ?
                <TestResult
                    score={ score }
                    maxScore={ tasks.length }

                    title="Угадайте, кто это написал: великий классик или алгоритм"

                    shortMessage={ result.shortMessage }
                    message={ result.message }
                    snippetText={ result.snippetText }
                    shareText={ result.shareText }
                    testPath={ urls.languageModels.tolstoy }

                    onRetryButtonClick={ onRetry }
                /> :
                <GeneratedTest
                    tasks={ tasks }

                    checkAnswer={ checkAnswer }
                    onTestFinished={ onTestFinished }
                />
            }
        </>
    );
};
