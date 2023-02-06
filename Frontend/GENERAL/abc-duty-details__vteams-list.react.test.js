import React from 'react';
import { shallow } from 'enzyme';

import VteamsList from 'b:abc-duty-details e:vteams-list';

describe('Should render VTeams List', () => {
    it('Should render VTeams with no items', () => {
        const wrapper = shallow(
            <VteamsList
                vteams={[]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render VTeams with one', () => {
        const wrapper = shallow(
            <VteamsList
                vteams={['vteams item']}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render VTeams with some items', () => {
        const wrapper = shallow(
            <VteamsList
                vteams={['vteams item1', 'vteams item2', 'vteams item3']}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
