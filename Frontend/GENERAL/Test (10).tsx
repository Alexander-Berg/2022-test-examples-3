// @ts-nocheck
import React, { useCallback, useState, useEffect } from 'react';
import { animated, useSpring } from '@react-spring/web';
import { useDrag } from '@use-gesture/react';
import sampleSize from 'lodash/sampleSize';
import { cn } from '@bem-react/classname';

import { useTestController } from 'hooks/useTestController';
import { CheckAnswerFn, IOption } from 'hooks/useTestController/types';

import { TestResult } from 'components/Tests/TestResult/TestResult';
import { ProgressBar } from 'components/Tests/ProgressBar/ProgressBar';

import CorrectIcon from 'assets/tests/icons/swipeCorrect.svg';
import WrongIcon from 'assets/tests/icons/swipeWrong.svg';

import data from './data.json';

import './Test.scss';
import { urls } from '../../../urls';

const b = cn('TranslationTest');

interface TranslationTask {
    question: {
        text: string;
        original: string;
        translated: string;
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

const checkTaskAnswer = (option: IOption, task: TranslationTask) => {
    return Promise.resolve({
        isCorrect: task.answer === option.key,
        correctAnswer: task.answer,
    });
};

const NUM_TASKS = 10;
const getRandomizedTasks = () =>
    sampleSize(data.items, NUM_TASKS).map(item => {
        const source = Math.random() >= 0.5 ? 'authentic' : 'generated';

        return {
            question: {
                text: 'Попробуйте отличить машинный перевод от сделанного человеком',
                original: item.original,
                translated: source === 'authentic' ? item.human : item.machine,
            },
            options: [
                {
                    text: 'Человек',
                    key: 'authentic',
                },
                {
                    text: 'Машина',
                    key: 'generated',
                },
            ],
            answer: source,
        };
    });

const isTouchLayout = window.innerWidth <= 768;

const MOVEMENT_TO_ROTATION_COEF = 0.1;
const X_SWIPE_ANIMATION_DISTANCE = isTouchLayout ? 300 : 600;
const Z_SWIPE_ANIMATION_ROTATION = isTouchLayout ? 30 : 45;
const TASK_SWITCH_DELAY = 500;

const useDragConfig = {
    swipe: {
        duration: 2000,
        distance: 10,
    },
};

interface TestProps {
    tasks: TranslationTask[];
    checkAnswer: CheckAnswerFn<TranslationTask>;
    onTestFinished: (score: number) => void;
}

export const TranslationTestComponent: React.FC<TestProps> = ({
    tasks,

    checkAnswer,
    onTestFinished,
}) => {
    const {
        score,
        result,
        userAnswerKey,
        currentQuestion,
        totalQuestions,
        task,

        onOptionSelect,
    } = useTestController({
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
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [task.question]);

    const bind = useDrag(e => {
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
                <animated.div style={styles} {...bind()} className={b('Card')}>
                    <div className={b('Original')}>{task.question.original}</div>
                    <div className={b('Translated')}>{task.question.translated}</div>
                </animated.div>
                <div className={b('CardsOverlay', { highlight })}>
                    {highlight === 'correct' && <CorrectIcon className={b('Icon')} />}
                    {highlight === 'incorrect' && <WrongIcon className={b('Icon')} />}
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
                    >
                        {option.text}
                    </div>
                ))}
            </div>
        </div>
    );
};

interface Props {
    onRetry: () => void;
}

const testTasks = getRandomizedTasks();

export const TranslationTest: React.FC<Props> = ({ onRetry }) => {
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
                    title="Машинный перевод или человеческий?"
                    shortMessage={result.shortMessage}
                    message={result.message}
                    testPath={urls['machine-translation']['break-a-leg']}
                    onRetryButtonClick={onRetry}
                />
            ) : (
                <TranslationTestComponent
                    tasks={testTasks}
                    onTestFinished={onTestFinished}
                    checkAnswer={checkTaskAnswer}
                />
            )}
        </>
    );
};
