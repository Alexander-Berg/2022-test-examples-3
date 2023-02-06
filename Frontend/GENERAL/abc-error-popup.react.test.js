import React from 'react';
import { shallow } from 'enzyme';

import AbcErrorPopup from 'b:abc-error-popup';

describe('AbcErrorPopup', () => {
    it('Should render abc-error-popup', () => {
        const wrapper = shallow(
            <AbcErrorPopup
                data={{ message: 'error' }}
                onClose={function() { /* Noop */ }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
