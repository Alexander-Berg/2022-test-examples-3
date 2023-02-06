import React from 'react';

import { shallow } from 'enzyme';
import { FullScreen } from './FullScreen';

describe('UI/FullScreen', () => {
    it('should render closed FullScreen window', () => {
        const wrapper = shallow(
            <FullScreen object={{}} fullscreen={false} setFullscreen={() => null}>
                content
            </FullScreen>,
        );
        expect(wrapper).toMatchSnapshot();
    });

    it('should render open FullScreen window', () => {
        const wrapper = shallow(
            <FullScreen object={{}} fullscreen setFullscreen={() => null}>
                content
            </FullScreen>,
        );
        expect(wrapper).toMatchSnapshot();
    });
});
