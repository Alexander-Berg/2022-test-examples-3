import React, { useCallback, useState } from 'react';
import { cn } from '@bem-react/classname';
import CorrectIcon from '../../../assets/tests/icons/correctIcon.svg';
import WrongIcon from '../../../assets/tests/icons/wrongIcon.svg';

import { urls } from '../../../../urls';

import { Test } from '../Test/Test';
import { SaliencySentence } from '../SaliencySentence/SaliencySentence';
import { TestResult } from '../TestResult/TestResult';

import data from './data_with_saliency.json';

import './YalmTest.css';

const cnYalmOption = cn('YalmOption');
const cnYalmQuestion = cn('YalmQuestion');

const renderIcon = isCorrect =>
    isCorrect ? (
        <CorrectIcon className={cnYalmOption('Icon')} />
    ) : (
        <WrongIcon className={cnYalmOption('Icon')} />
    );

const YalmTestOption = ({ option, onClick, correct, hasResult, isPicked, answerMessage }) => {
    const { text, source } = option;
    const handleClick = useCallback(() => onClick(option), [onClick, option]);

    return (
        <div className={cnYalmOption({ correct, isPicked })} onClick={handleClick}>
            <div className={cnYalmOption('Text')}>
                {hasResult ? (
                    <SaliencySentence tokens={option.tokens} saliencyInfo={option.saliency} />
                ) : (
                    text
                )}
            </div>

            {hasResult && (
                <div className={cnYalmOption('Footer')}>
                    <div className={cnYalmOption('Source')}>
                        {(isPicked || correct) && renderIcon(correct)}
                        {source}
                    </div>
                    {isPicked && answerMessage && (
                        <div className={cnYalmOption('PickedText')}>{answerMessage}</div>
                    )}
                </div>
            )}
        </div>
    );
};

const YalmQuestion = ({ question }) => {
    return (
        <div className={cnYalmQuestion()}>
            <div className={cnYalmQuestion('Scheme')}>
                <SaliencySentence
                    tokens={question.tokens}
                    saliencyInfo={question.saliency}
                    hideText
                    noLeading
                />
            </div>

            <div className={cnYalmQuestion('Text')}>Какая из новостей описана цветовой схемой?</div>
        </div>
    );
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

const tasks = data.tasks;

export const YalmTest = ({ onRetry }) => {
    const [finished, setFinished] = useState(false);
    const [score, setScore] = useState(null);

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
                    title="Тест: посмотрите на русский язык с точки зрения языковой модели"
                    shortMessage={result.shortMessage}
                    message={result.message}
                    snippetText={result.snippetText}
                    shareText={result.shareText}
                    testPath={urls.languageModels.read}
                    onRetryButtonClick={onRetry}
                />
            ) : (
                <Test
                    OptionComponent={YalmTestOption}
                    QuestionComponent={YalmQuestion}
                    tasks={tasks}
                    onTestFinished={onTestFinished}
                />
            )}
        </>
    );
};
