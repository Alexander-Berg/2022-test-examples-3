import React from 'react';
import { shallow } from 'enzyme';

import VteamsItem from 'b:abc-duty-details e:vteams-item';

describe('Should render VTeams Item or Title', () => {
    it('Should render VTeams Item', () => {
        const wrapper = shallow(
            <VteamsItem
                item="vteams item"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render Last Item with comma', () => {
        const wrapper = shallow(
            <VteamsItem
                item="vteams item"
                needComma
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
