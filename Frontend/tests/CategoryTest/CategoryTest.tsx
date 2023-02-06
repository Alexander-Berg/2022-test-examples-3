import React, { useCallback } from 'react';
import { cn } from '@bem-react/classname';
import './CategoryTest.css';

import type { Categories, CheckAnswerFn, Task } from 'hooks/useCategoryTestController/types';
import { useCategoryTestController } from 'hooks/useCategoryTestController';

import { Button } from '../Button/Button';
import { ProgressBar } from '../ProgressBar/ProgressBar';

import { Question, QuestionProps } from '../Question/Question';
import { Option, OptionProps } from './Option/Option';

import { defaultCheckAnswer } from './utils';

interface Props {
    OptionComponent?: React.ComponentType<OptionProps>;
    QuestionComponent?: React.ComponentType<QuestionProps>;
    tasks: Task[];
    checkAnswer?: CheckAnswerFn;
    taskSwitchMode?: 'auto' | 'manual';
    onTestFinished: (categories: Categories) => void;
}

const b = cn('CategoryTest');

export const CategoryTest: React.FC<Props> = ({
    OptionComponent = Option,
    QuestionComponent = Question,

    tasks,
    taskSwitchMode = 'manual',

    checkAnswer = defaultCheckAnswer,
    onTestFinished,
}) => {
    const {
        currentQuestion,
        totalQuestions,
        result,
        userAnswerKey,
        categories,
        task,

        onOptionSelect,
        openNextTask,
    } = useCategoryTestController({
        tasks,
        checkAnswer,
        onTestFinished,
        taskSwitchMode,
    });

    const handleNextButtonClick = useCallback(() => {
        openNextTask(categories);
    }, [categories, openNextTask]);

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
                        hasResult={Boolean(result)}
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
