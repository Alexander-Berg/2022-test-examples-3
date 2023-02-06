import { storiesOf } from '@storybook/react';
import React from 'react';
import { Provider as StoreProvider } from 'react-redux';

import ProblemTestsets from 'client/components/problem-testsets';
import {
    problemId,
    store,
    storeWithAddTestsetStarted,
    storeWithValidity,
} from 'client/components/problem-testsets/__stories__/constants';
import createStore from 'client/store';

storiesOf('Problems | Testsets', module)
    .add('Default', () => (
        <StoreProvider store={createStore({ state: store }).store}>
            <ProblemTestsets problemId={problemId} />
        </StoreProvider>
    ))
    .add('With validity', () => (
        <StoreProvider store={createStore({ state: storeWithValidity }).store}>
            <ProblemTestsets problemId={problemId} />
        </StoreProvider>
    ))
    .add('Add testset started', () => (
        <StoreProvider store={createStore({ state: storeWithAddTestsetStarted }).store}>
            <ProblemTestsets problemId={problemId} />
        </StoreProvider>
    ));
