import React, { useCallback, useState, useEffect } from 'react';
import { animated, useSpring } from '@react-spring/web';
import { useDrag } from '@use-gesture/react';
import sampleSize from 'lodash/sampleSize';
import { cn } from '@bem-react/classname';

import { urls } from '../../../urls';

import { useTestController } from 'hooks/useTestController';
import { CheckAnswerFn, IOption } from 'hooks/useTestController/types';

import { TestResult } from 'components/Tests/TestResult/TestResult';
import { ProgressBar } from 'components/Tests/ProgressBar/ProgressBar';
import { Spinner } from 'components/Spinner/Spinner';

import data from './data.json';

import './Test.scss';

const b = cn('TranslationTest');

interface WhatIsThatTask {
    question: {
        text: string;
    };
    images: {
        desktop: {
            question: string;
            correct: string;
            incorrect: string;
        };
        touch: {
            question: string;
            correct: string;
            incorrect: string;
        }
    };
    options: IOption[];
    answer: string;
}

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

const checkTaskAnswer = (option: IOption, task: WhatIsThatTask) => {
    return Promise.resolve({
        isCorrect: task.answer === option.key,
        correctAnswer: task.answer,
    });
};

const NUM_TASKS = 10;
const getRandomizedTasks = (): WhatIsThatTask[] =>
    sampleSize(data.items, NUM_TASKS).map(item => {
        return {
            question: {
                text: 'Угадайте, какой предмет перед вами',
            },
            images: item.images,
            options: item.options,
            answer: item.answer,
        };
    });

const isTouchLayout = window.innerWidth <= 768;

const MOVEMENT_TO_ROTATION_COEF = 0.1;
const X_SWIPE_ANIMATION_DISTANCE = isTouchLayout ? 300 : 600;
const Z_SWIPE_ANIMATION_ROTATION = isTouchLayout ? 30 : 45;
const TASK_SWITCH_DELAY = 750;

const useDragConfig = {
    swipe: {
        duration: 2000,
        distance: 10,
    },
};

interface TestProps {
    tasks: WhatIsThatTask[];
    checkAnswer: CheckAnswerFn<WhatIsThatTask>;
    onTestFinished: (score: number) => void;
}

const getImage = (images: WhatIsThatTask['images'], key: keyof WhatIsThatTask['images']['desktop']) => {
    if (isTouchLayout) {
        return images.touch[key];
    }

    return images.desktop[key];
};

export const WhatIsThatTestComponent: React.FC<TestProps> = ({
    tasks,

    checkAnswer,
    onTestFinished,
}) => {
    const [imageLoaded, setImageLoaded] = useState(false);
    const {
        score,
        result,
        userAnswerKey,
        currentQuestion,
        totalQuestions,
        task,

        onOptionSelect,
    } = useTestController<WhatIsThatTask>({
        tasks,
        checkAnswer,
        onTestFinished,
        taskSwitchMode: 'auto',
        taskSwitchDelay: TASK_SWITCH_DELAY,
    });

    const [styles, api] = useSpring(() => ({
        from: { x: 0, y: 0, rotateZ: 0, opacity: 1 },
        immediate: true,
    }));

    const handleOptionSelect = useCallback(
        index => {
            onOptionSelect(task.options[index]);
        },
        [onOptionSelect, task.options],
    );

    useEffect(() => {
        if (userAnswerKey) {
            const direction =
                task.options.findIndex(({ key }) => key === userAnswerKey) === 0 ? -1 : 1;

            api({
                x: X_SWIPE_ANIMATION_DISTANCE * direction,
                rotateZ: Z_SWIPE_ANIMATION_ROTATION * direction,
                opacity: 0,
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [result]);

    useEffect(() => {
        api({
            x: 0,
            y: 0,
            rotateZ: 0,
            opacity: 1,
            immediate: true,
        });
        setImageLoaded(false);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [task.images]);

    const bind = useDrag(e => {
        e.event.preventDefault();

        if (e.swipe[0]) {
            return handleOptionSelect(e.swipe[0] === -1 ? 0 : 1);
        }

        api({
            x: e.down ? e.movement[0] : 0,
            y: e.down ? e.movement[1] : 0,
            rotateZ: e.down ? e.movement[0] * MOVEMENT_TO_ROTATION_COEF : 0,
        });
    }, useDragConfig);

    // eslint-disable-next-line no-nested-ternary
    const highlight = result ? (result.isCorrect ? 'correct' : 'incorrect') : false;

    return (
        <div className={b()}>
            <div className={b('Progress')}>
                <div className={b('Score')}>Очки: {score}</div>
                <ProgressBar progress={((currentQuestion - 1) / totalQuestions) * 100} />
            </div>

            <div className={b('Question')}>
                <div className={b('Text')}>{task.question.text}</div>
            </div>

            <div className={b('Cards')}>
                <Spinner
                    className={b('Loader')}
                    progress={!imageLoaded}
                />
                <animated.div style={styles} {...bind()} className={b('Card')}>
                    <img
                        src={getImage(task.images, 'question')}
                        className={b('Image', { loaded: imageLoaded })}
                        onLoad={() => setImageLoaded(true)}
                    />
                </animated.div>
                <div className={b('CardsOverlay', { highlight })}>
                    {highlight === 'correct' && <img className={b('Answer')} src={getImage(task.images, 'correct')} />}
                    {highlight === 'incorrect' && <img className={b('Answer')} src={getImage(task.images, 'incorrect')} />}
                </div>
            </div>

            <div className={b('Options')}>
                {task.options.map((option, idx) => (
                    <div
                        className={b('Option', {
                            highlighted: Boolean(result && userAnswerKey === option.key),
                            correct: Boolean(result && result.isCorrect),
                        })}
                        key={idx}
                        onClick={() => handleOptionSelect(idx)}
                        dangerouslySetInnerHTML={{ __html: option.text }}
                    />
                ))}
            </div>
        </div>
    );
};

interface Props {
    onRetry: () => void;
}

const testTasks = getRandomizedTasks();

export const WhatIsThatTest: React.FC<Props> = ({ onRetry }) => {
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
                    maxScore={testTasks.length}
                    title="Смотри как машина"
                    shortMessage={result.shortMessage}
                    message={result.message}
                    testPath={urls['self-driving']['what-is-that']}
                    onRetryButtonClick={onRetry}
                />
            ) : (
                <WhatIsThatTestComponent
                    tasks={testTasks}
                    onTestFinished={onTestFinished}
                    checkAnswer={checkTaskAnswer}
                />
            )}
        </>
    );
};
