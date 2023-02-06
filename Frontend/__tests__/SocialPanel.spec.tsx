import * as React from 'react';
import { shallow } from 'enzyme';

import { SocialPanel } from '../SocialPanel';

describe('SocialPanel', () => {
    test('Рендерится без ошибок', () => {
        const wrapper = shallow(
            <SocialPanel
                className="some-class"
                onButtonClick={jest.fn()}
                onWrapperClick={jest.fn()}
            >
                some text
            </SocialPanel>
        );

        expect(wrapper).toMatchSnapshot();
    });
});
