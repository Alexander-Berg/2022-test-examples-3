import { storiesOf, specs, describe, snapshot } from '../.storybook/facade';
import React from 'react';
import IndicatorBar from '../../../components/redux/components/indicator-bar';
import '../../../components/redux/components/indicator-bar/index.styl';

const getComponent = (props) => (<IndicatorBar {...props} />);

export default storiesOf('PaymentCancelDialog', module)
    .add('30%', ({ kind, story }) => {
        const component = getComponent({ value: 30 });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
        return component;
    })
    .add('85%', ({ kind, story }) => {
        const component = getComponent({ value: 85 });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
        return component;
    })
    .add('99%', ({ kind, story }) => {
        const component = getComponent({ value: 99 });

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));
        return component;
    });
