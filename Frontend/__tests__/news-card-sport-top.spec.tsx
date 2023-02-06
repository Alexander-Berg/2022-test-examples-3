import * as React from 'react';
import { shallow } from 'enzyme';
import { NewsCardSportTop as NewsCardSportTopPhone } from '../NewsCardSportTop@phone';
import { NewsCardSportTop as NewsCardSportTopDesktop } from '../NewsCardSportTop@desktop';
import * as data from '../datastub';

describe('NewsCardSportTop component', () => {
    it('should render without crashing (phone)', () => {
        const wrapper = shallow(
            <NewsCardSportTopPhone {...data.dataDefault} />
        );
        expect(wrapper.length).toEqual(1);
    });

    it('should render without crashing (desktop)', () => {
        const wrapper = shallow(
            <NewsCardSportTopDesktop {...data.dataDefault} />
        );
        expect(wrapper.length).toEqual(1);
    });
});
