import { storiesOf } from '@storybook/react';
import React, { createElement, useState, ComponentType, Fragment } from 'react';

import ProblemTest from 'client/components/problem-test';
import { ITestUpdate, Props } from 'client/components/problem-test/types';

interface StoryComponentProps {
    variants: Props['variants'];
    answer: Props['answer'];
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const withStoryComponent = (ComposedComponent: ComponentType<any>) => ({
    variants = [],
    answer = [],
}: StoryComponentProps) => {
    const [test, changeTest] = useState({ answer, variants } as ITestUpdate);

    return (
        <Fragment>
            <ComposedComponent
                variants={test.variants}
                answer={test.answer}
                onUpdate={changeTest}
            />
            <div>Варианты: {test.variants.toString()}</div>
            <div>
                Ответы: {test.answer.map((answerIdx) => test.variants[answerIdx - 1]).toString()}
            </div>
        </Fragment>
    );
};

const withSingle = (ComposedComponent: ComponentType<Props>) => (props: Props) =>
    createElement(ComposedComponent, { multi: false, ...props });

storiesOf('Problems | Test Answer', module)
    .add('Multi', () => createElement(withStoryComponent(ProblemTest)))
    .add('Single', () => createElement(withStoryComponent(withSingle(ProblemTest))))
    .add('With variants', () =>
        createElement(withStoryComponent(ProblemTest), {
            variants: ['Земля', 'Венера', 'Меркурий'],
            answer: [1],
        }),
    );
