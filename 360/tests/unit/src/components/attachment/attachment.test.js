import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';
import Attachment from '../../../../../src/components/attachment';
import { Spin } from '@ps-int/ufo-rocks/lib/components/lego-components/Spin';
import getStore from '../../../../../src/store';
import { STATES } from '../../../../../src/consts';

jest.mock('../../../../../src/store/actions', () => ({
    openDialog: jest.fn(() => ({ type: '' }))
}));

import { openDialog } from '../../../../../src/store/actions';

const onAttachmentOpen = jest.fn();
const getAttachmentProps = ({ preview, state }) => ({
    id: 'id',
    noteId: 'noteId',
    attachment: {
        resourceId: 'resourceId',
        preview,
        state
    },
    openOnSingleClick: true,
    onOpen: onAttachmentOpen
});
const getComponent = ({
    preview = 'preview',
    state = STATES.INITIAL
}) => (
    <Provider store={getStore({})}>
        <Attachment {...getAttachmentProps({ preview, state })} />
    </Provider>
);

describe('component/attachment =>', () => {
    it('should render spinner when an attachment is being created', () => {
        const wrapper = mount(getComponent({ state: STATES.CREATING }));

        expect(wrapper.find(Spin).exists()).toBe(true);
        expect(wrapper.find('.attachment__controls').exists()).toBe(false);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should render spinner when an attachment is being loaded', () => {
        const wrapper = mount(getComponent({ state: STATES.LOADING }));

        expect(wrapper.find(Spin).exists()).toBe(true);
        expect(wrapper.find('.attachment__controls').exists()).toBe(false);
    });

    it('should render control buttons when an attachment is not being loaded or created on desktop device', () => {
        const wrapper = mount(getComponent({}));

        expect(wrapper.find(Spin).exists()).toBe(false);
        expect(wrapper.find('.attachment__controls').exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should not render control buttons when an attachment is not being loaded or created on touch device', () => {
        global.IS_TOUCH = true;

        const wrapper = mount(getComponent({}));

        expect(wrapper.find(Spin).exists()).toBe(false);
        expect(wrapper.find('.attachment__controls').exists()).toBe(false);
        expect(wrapper.render()).toMatchSnapshot();
        global.IS_TOUCH = false;
    });

    it('should call pointer events callbacks passed as props if an attachment has a preview', () => {
        onAttachmentOpen.mockReset();

        const wrapper = mount(getComponent({}));

        expect(onAttachmentOpen).not.toBeCalled();
        wrapper.find('.attachment').first().simulate('click');
        expect(onAttachmentOpen).toBeCalledTimes(1);
    });

    it('should not call pointer events callbacks passed as props if an attachment has no preview', () => {
        onAttachmentOpen.mockReset();

        const wrapper = mount(getComponent({ preview: null }));

        wrapper.find('.attachment').first().simulate('click');
        expect(onAttachmentOpen).not.toBeCalled();
    });

    it('should call open dialog action by trying to delete an attachment', () => {
        const wrapper = mount(getComponent({}));

        wrapper.find('.attachment__controls').children().first().simulate('click');
        expect(popFnCalls(openDialog)[0]).toEqual([
            'attachmentDeleteConfirmation',
            { resourceId: getAttachmentProps({}).id, source: 'from list' }
        ]);
    });
});
