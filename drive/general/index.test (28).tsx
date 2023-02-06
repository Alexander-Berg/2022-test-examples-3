import { mount } from 'enzyme';
import * as React from 'react';

import Radio from './index';

describe('Radio button', () => {
    it('should render properly (checked)', () => {
        const buttonGroup = mount(
            <Radio onSelect={() => undefined}
                   value={'test'}
                   id={'1'}
                   selectedValue={'test'}
                   groupId={'1'}>
                Test
            </Radio>,
        );
        expect(buttonGroup).toMatchSnapshot();
    });

    it('should render properly (unchecked)', () => {
        const buttonGroup = mount(
            <Radio onSelect={() => undefined}
                   value={'test'}
                   id={'2'}
                   selectedValue={'1'}
                   groupId={'2'}>
                Test 2
            </Radio>,
        );
        expect(buttonGroup).toMatchSnapshot();
    });
});
