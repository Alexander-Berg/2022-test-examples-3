import { storiesOf } from '@storybook/react';
import flow from 'lodash/flow';
import React, { ComponentType } from 'react';
import { Provider } from 'react-redux';
import { withRouter, BrowserRouter as Router, Route } from 'react-router-dom';

import { IProblem } from 'common/types/problem';

import ProblemsetTesting from 'client/components/contest-problemset/problemset-testing';
import { problemsGenerator } from 'client/components/contest-problemset/problemset-testing/__stories__/helper';
import createStore from 'client/store';

function generateProblems(isEmpty: boolean = false, count: number = 150): IProblem[] {
    return Array.from(problemsGenerator(count, isEmpty));
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const stubState = (ComposedComponent: ComponentType<any>) => (props: any) => (
    <ComposedComponent
        problemset={{ problems: generateProblems(false) }}
        totalItems={150}
        {...props}
    />
);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const emptyStubState = (ComposedComponent: ComponentType<any>) => (props: any) => (
    <ComposedComponent
        problemset={{ problems: generateProblems(true) }}
        totalItems={0}
        {...props}
    />
);

storiesOf('Contest Problemset | Testing', module)
    .add('default', () => (
        <Provider store={createStore({ state: {} }).store}>
            <Router>
                <Route component={flow([stubState, withRouter])(ProblemsetTesting)} />
            </Router>
        </Provider>
    ))
    .add('without tests sets and checkers', () => (
        <Provider store={createStore({ state: {} }).store}>
            <Router>
                <Route component={flow([emptyStubState, withRouter])(ProblemsetTesting)} />
            </Router>
        </Provider>
    ));
