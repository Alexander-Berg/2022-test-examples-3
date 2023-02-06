import React from 'react';
import { mount } from 'enzyme';

import Modal from 'b:modal m:theme=normal m:has-cross=yes';

describe('Modal', () => {
// задача на расскип https://st.yandex-team.ru/ABC-6592
// eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Should render modal with a cross button', () => {
        jest.useFakeTimers();

        const myRef = React.createRef();
        const onClick = jest.fn();

        const wrapper = mount(
            <div>
                <div ref={myRef} />
                <Modal
                    theme="normal"
                    visible
                    hasCross
                    onCrossClick={onClick}
                    scope={() => myRef.current}
                >
                    <p>Hello world!</p>
                </Modal>
            </div>
        );

        jest.runAllTimers();

        expect(wrapper).toMatchSnapshot();

        wrapper.find('.modal__cross').simulate('click');
        expect(onClick).toHaveBeenCalled();

        wrapper.unmount();
    });
});
