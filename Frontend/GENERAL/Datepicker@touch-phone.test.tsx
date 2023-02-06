import * as React from 'react';
import { mount } from 'enzyme';
import { compose } from '@bem-react/core';
import { Registry, withRegistry } from '@bem-react/di';
import { Textinput } from '@yandex-lego/components/Textinput/touch-phone/bundle';
import { Icon } from '../Icon/Icon.bundle/touch-phone';
import { withTypeCalendar } from '../Icon/_type/Icon_type_calendar';

import { Datepicker as DatepickerPresenter, cnDatepicker } from './Datepicker@touch-phone';

const datepickerRegisty = new Registry({ id: cnDatepicker() });
const CalendarIcon = compose(withTypeCalendar)(Icon);

datepickerRegisty.set('Textinput', Textinput);
datepickerRegisty.set('CalendarIcon', CalendarIcon);

const Datepicker = withRegistry(datepickerRegisty)(DatepickerPresenter);

describe('Datepicker@touch-phone', () => {
    it('Should render empty component', () => {
        const wrapper = mount(<Datepicker />);

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should support Textinput / Input props', () => {
        const wrapper = mount(
            <Datepicker
                view="default"
                size="m"
                pin="brick-brick"
                disabled
                readOnly
                name="datefield"
                min={new Date(Date.UTC(2015, 1, 15))}
                max={new Date(Date.UTC(2015, 1, 18))}
                value={new Date(Date.UTC(2015, 1, 17))}
            />,
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });
});
