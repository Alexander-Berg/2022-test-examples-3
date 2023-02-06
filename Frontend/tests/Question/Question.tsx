import React from 'react';
import { cn } from '@bem-react/classname';

import type { IQuestion } from 'hooks/useTestController/types';

import './Question.scss';

export const cnTestQuestion = cn('TestQuestion');

export interface QuestionProps<QuestionType extends IQuestion = IQuestion> {
    question: QuestionType;
    hasResult?: boolean;
}

export const Question: React.FC<QuestionProps> = ({ question }) => {
    return (
        <div className={cnTestQuestion()}>
            {question.title && <div className={cnTestQuestion('Title')}>{question.title}</div>}
            <div className={cnTestQuestion('Text')}>
                {question.text.split('\n').map((text, i) => (
                    <div key={i} dangerouslySetInnerHTML={{ __html: text }} />
                ))}
            </div>
        </div>
    );
};
