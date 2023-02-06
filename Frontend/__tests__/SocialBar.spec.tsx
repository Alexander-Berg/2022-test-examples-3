import * as React from 'react';
import { shallow } from 'enzyme';
import { SocialBar } from '../SocialBar';

// @ts-ignore
global.window.Ya = {};

describe('Компонент SocialBar', () => {
    test('Совпадает со снепшотом', () => {
        const Child = () => <div className="child">Child</div>;
        const wrapper = shallow(
            <SocialBar pageHash="hash" className="mixed">
                <Child />
            </SocialBar>
        );

        expect(wrapper).toMatchSnapshot();
    });
});
