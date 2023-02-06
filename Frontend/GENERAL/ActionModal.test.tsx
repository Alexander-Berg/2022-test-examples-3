import React from 'react';

import { shallow } from 'enzyme';
import { ActionModal } from './ActionModal';

const action = { type: 'apply', title: 'Title', view: 'action', size: 's', onClick: () => null } as const;

describe('UI/ActionModal', () => {
    it('should render hidden ActionModal', () => {
        const wrapper = shallow(<ActionModal title="ActionModal" visible={false} onClose={() => null} actions={[action]} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render visible ActionModal', () => {
        const wrapper = shallow(<ActionModal title="ActionModal" visible onClose={() => null} actions={[action]} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render ActionModal with specified scope', () => {
        const wrapper = shallow(<ActionModal title="ActionModal" visible onClose={() => null} actions={[action]} />);
        expect(wrapper).toMatchSnapshot();
    });
});
