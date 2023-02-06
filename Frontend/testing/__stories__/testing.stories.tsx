import { storiesOf } from '@storybook/react';
import React from 'react';

import Testing from 'client/components/contest-settings/testing';

const stubFunction = () => () => ({});

storiesOf('Contest Settings | Testing', module)
    .add('default', () => (
        <Testing
            reportSettings={{ stopOnSampleFail: true, useAcNotOk: true }}
            onChange={stubFunction}
        />
    ))
    .add('not stop on first fail', () => (
        <Testing
            reportSettings={{ stopOnSampleFail: false, useAcNotOk: true }}
            onChange={stubFunction}
        />
    ))
    .add('not use AC not OK', () => (
        <Testing
            reportSettings={{ stopOnSampleFail: true, useAcNotOk: false }}
            onChange={stubFunction}
        />
    ));
