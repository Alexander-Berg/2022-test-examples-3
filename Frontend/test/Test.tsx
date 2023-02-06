import React, { useCallback } from 'react';
import { cn } from '@bem-react/classname';
import './Test.css';

import type { CheckAnswerFn, Task } from 'hooks/useTestController/types';
import { useTestController } from 'hooks/useTestController';

import { Button } from '../Button/Button';
import { ProgressBar } from '../ProgressBar/ProgressBar';

import { Question, QuestionProps } from '../Question/Question';
import { Option, OptionProps } from './Option/Option';

import { defaultCheckAnswer, getOptionAnswerMessage } from './utils';

interface Props {
    OptionComponent?: React.ComponentType<OptionProps>;
    QuestionComponent?: React.ComponentType<QuestionProps>;
    tasks: Task[];
    checkAnswer?: CheckAnswerFn;
    onTestFinished: (score: number) => void;
}

const b = cn('Test');

export const Test: React.FC<Props> = ({
    OptionComponent = Option,
    QuestionComponent = Question,

    tasks,

    checkAnswer = defaultCheckAnswer,
    onTestFinished,
}) => {
    const {
        currentQuestion,
        totalQuestions,
        result,
        userAnswerKey,
        score,
        task,

        onOptionSelect,
        openNextTask,
    } = useTestController({
        tasks,
        checkAnswer,
        onTestFinished,
    });

    const handleNextButtonClick = useCallback(() => {
        openNextTask(score);
    }, [score, openNextTask]);

    return (
        <div className={b()}>
            <div className={b('Progress')}>
                <div className={b('Score')}>
                    {currentQuestion} / {totalQuestions}
                </div>
                <ProgressBar progress={((currentQuestion - 1) / totalQuestions) * 100} />
            </div>

            <div className={b('Question')}>
                <QuestionComponent question={task.question} hasResult={Boolean(result)} />
            </div>

            <div className={b('Options')}>
                {task.options.map((option, idx) => (
                    <OptionComponent
                        key={idx}
                        option={option}
                        onClick={onOptionSelect}
                        isPicked={userAnswerKey === option.key}
                        correct={Boolean(result && result.correctAnswer === option.key)}
                        hasResult={Boolean(result)}
                        answerMessage={
                            result &&
                            getOptionAnswerMessage(option.key === result.correctAnswer, task)
                        }
                        pickedText={option.comment || task.pickedText}
                    />
                ))}
            </div>

            <div className={b('Controls')}>
                <Button onClick={handleNextButtonClick} disabled={!result}>
                    Дальше
                </Button>
            </div>
        </div>
    );
};
