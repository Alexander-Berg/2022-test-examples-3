import React from 'react';
import { mount } from 'enzyme';
import NoteStateIndicator, { StatusNofitication } from '../../../../../src/components/note-state-indicator';
import { CURRENT_NOTE_STATE } from '../../../../../src/consts';

const getProps = ({ status = CURRENT_NOTE_STATE.DEFAULT, hidden = false }) => {
    return {
        status,
        cls: '',
        options: { symbols: 0 },
        hidden
    };
};
const getComponent = (props) => <NoteStateIndicator {...props} />;

describe('components/note-state-indicator =>', () => {
    it('should not render at all if hidden', () => {
        const wrapper = mount(getComponent(getProps({ hidden: true })));

        expect(wrapper.isEmptyRender()).toBe(true);
    });

    it('should not render any status message by default', () => {
        const wrapper = mount(getComponent(getProps({})));

        expect(wrapper.find('.status-indicator').children().exists()).toBe(false);
    });

    it('should render an appropriate message if current note state indicates size limit excess', () => {
        const wrapper = mount(getComponent(getProps({})));

        wrapper.setProps(getProps({ status: CURRENT_NOTE_STATE.SIZE_LIMIT_EXCEEDED }));
        wrapper.update();
        expect(wrapper.find(StatusNofitication).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should render an appropriate message if current note state indicates saving', () => {
        const wrapper = mount(getComponent(getProps({})));

        wrapper.setProps(getProps({ status: CURRENT_NOTE_STATE.SAVING }));
        wrapper.update();
        expect(wrapper.find(StatusNofitication).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should render an appropriate message if current note state indicates loading', () => {
        const wrapper = mount(getComponent(getProps({})));

        wrapper.setProps(getProps({ status: CURRENT_NOTE_STATE.LOADING }));
        wrapper.update();
        expect(wrapper.find(StatusNofitication).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
    });
});
