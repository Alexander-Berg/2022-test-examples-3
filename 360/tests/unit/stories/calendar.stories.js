
import { storiesOf, specs, describe, it, snapshot, mount, mockFunction, wait } from '../.storybook/facade';
import React from 'react';

import Calendar from 'rocks/calendar';
import '../../../components/rocks/calendar/index.styl';
import { Button } from '@ps-int/ufo-rocks/lib/components/lego-components/Button';

const actions = {
    onChange: mockFunction()
};

export default storiesOf('Calendar', module)
    .add('New Year', ({ kind, story }) => {
        const component = <Calendar
            timestamp={(new Date(2018, 0, 1)).getTime()}
            onChange={actions.onChange}
        />;

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));

        specs(() => describe(kind, () => {
            const wrapper = mount(component);

            const prevButton = wrapper
                .find(Button)
                .findWhere((button) => button.prop('className').toString() === 'calendar__prev-button')
                .first();

            prevButton.simulate('click');

            const selectorDecember = () => wrapper.getByText(i18n('%ufo_calendar__month-names__11'));

            wait(selectorDecember).then(() => {
                it('calls changes month', () => {
                    expect(selectorDecember()).toBeInTheDOM();
                });
            });
        }));

        return component;
    });
