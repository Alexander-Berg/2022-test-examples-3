import type { CheckAnswerFn, Task } from 'hooks/useTestController/types';

export const defaultCheckAnswer: CheckAnswerFn = (option, task) => {
    const correctAnswer = task.answer;

    const result = {
        isCorrect: correctAnswer === option.key,
        correctAnswer,
    };

    return Promise.resolve(result);
};

export const getOptionAnswerMessage = (correct: boolean, task: Task) => {
    if (correct && task.correctAnswerMessage) {
        return task.correctAnswerMessage;
    }

    if (!correct && task.incorrectAnswerMessage) {
        return task.incorrectAnswerMessage;
    }

    return task.answerMessage;
};
