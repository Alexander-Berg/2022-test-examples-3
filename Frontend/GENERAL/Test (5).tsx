import React, { useCallback, useState } from 'react';

import { Test } from 'components/Tests/Test/Test';
import { TestResult } from 'components/Tests/TestResult/TestResult';
import { cnTestOption, renderIcon, OptionProps } from 'components/Tests/Test/Option/Option';

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

const Option: React.FC<OptionProps> = ({
    option,
    onClick,
    correct,
    isPicked,
    answerMessage,
}) => {
    const { text } = option;
    const handleClick = useCallback(() => onClick(option), [onClick, option]);

    return (
        <div className={cnTestOption({ correct, isPicked })} onClick={handleClick}>
            <div className={cnTestOption('Text')}>{text}</div>

            {(isPicked || correct) && (
                <div className={cnTestOption('Footer')}>
                    <div className={cnTestOption('PickedComment')}>
                        {(isPicked || correct) && renderIcon(correct)}
                        {answerMessage}
                    </div>
                    {option.pickedText && (
                        <div className={cnTestOption('PickedText')} dangerouslySetInnerHTML={{ __html: option.pickedText }} />
                    )}
                </div>
            )}
        </div>
    );
};

interface Props {
    onRetry: () => void;
}

export const IntoTheWildTest: React.FC<Props> = ({ onRetry }) => {
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
                    title="Тест: изучите прогноз погоды и угадайте, где вы находитесь"
                    shortMessage={result.shortMessage}
                    message={result.message}
                    testPath={urls.weather['into-the-wild']}
                    onRetryButtonClick={onRetry}
                />
            ) : (
                <Test tasks={tasks} onTestFinished={onTestFinished} OptionComponent={Option} />
            )}
        </>
    );
};
