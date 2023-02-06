import React from 'react';
import { mount } from 'enzyme';

import Modal from 'b:modal m:theme=normal m:abc-theme=normal';

describe('Modal', () => {
// задача на расскип https://st.yandex-team.ru/ABC-6592
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Should render modal with abc theme', () => {
        const wrapper = mount(
            <Modal
                theme="normal"
                abcTheme="normal"
            >
                <p>Hello world!</p>
            </Modal>
        );
        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
