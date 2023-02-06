import { storiesOf } from '@storybook/react';
import React from 'react';
import { Provider } from 'react-redux';

import Button from 'client/components/button';
import CreateTest from 'client/components/create-test';
import createStore from 'client/store';

storiesOf('Problems | Create Test', module).add('Default', () => (
    <Provider store={createStore({ state: {} }).store}>
        <CreateTest problemId="1">
            {(openModal) => (
                <Button theme="normal" size="m" onClick={openModal}>
                    Создать тест
                </Button>
            )}
        </CreateTest>
    </Provider>
));
