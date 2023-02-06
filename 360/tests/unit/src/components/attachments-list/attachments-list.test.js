import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';
import Carousel from '@ps-int/ufo-rocks/lib/components/carousel';
import AttachmentsList from '../../../../../src/components/attachments-list';
import Attachment from '../../../../../src/components/attachment';
import getStore from '../../../../../src/store';
import { DESKTOP_LAYOUT_THRESHOLD } from '../../../../../src/consts';

jest.mock('../../../../../src/helpers/metrika', () => ({
    countAttachment: jest.fn()
}));
jest.mock('../../../../../src/store/actions/attachments', () => ({
    toggleSlider: jest.fn(() => ({ type: '' }))
}));

import { toggleSlider } from '../../../../../src/store/actions/attachments';
import { countAttachment } from '../../../../../src/helpers/metrika';

const getState = ({ currentNoteId, currentNoteAttachments, currentNoteAttachmentOrder, windowWidth }) => ({
    notes: {
        current: currentNoteId,
        notes: {
            someId: {
                attachmentOrder: currentNoteAttachmentOrder,
                attachments: currentNoteAttachments
            }
        }
    },
    environment: { windowWidth }
});
const DEFAULT_ATTACHMENTS = {
    attachment1: { resourceId: 'attachment1' },
    attachment2: { resourceId: 'attachment2' },
    attachment3: { resourceId: 'attachment3', error: true, preview: 'preview' },
    attachment4: { resourceId: 'attachment4', error: true }
};
const DEFAULT_ATTACHMENT_ORDER = ['attachment1', 'attachment2', 'attachment3', 'attachment4'];
const getComponent = ({
    currentNoteId = 'someId',
    currentNoteAttachments = DEFAULT_ATTACHMENTS,
    currentNoteAttachmentOrder = DEFAULT_ATTACHMENT_ORDER,
    windowWidth = DESKTOP_LAYOUT_THRESHOLD
}) => (
    <Provider store={getStore(getState({
        currentNoteId,
        currentNoteAttachments,
        currentNoteAttachmentOrder,
        windowWidth
    }))}>
        <AttachmentsList />
    </Provider>
);

describe('components/attachments-list =>', () => {
    it('should not render anything if selected note does not have any attachments', () => {
        const wrapper = mount(getComponent({ currentNoteAttachmentOrder: [] }));

        expect(wrapper.isEmptyRender()).toBe(true);
    });

    it('should not render anything if there is no selected note', () => {
        const wrapper = mount(getComponent({ currentNoteId: null }));

        expect(wrapper.isEmptyRender()).toBe(true);
    });

    it('should not render attachments, which have errors and no previews', () => {
        const wrapper = mount(getComponent({}));

        expect(wrapper.find(Attachment).length).toBe(
            Object.values(DEFAULT_ATTACHMENTS).filter((attachment) => !(attachment.error && !attachment.preview)).length
        );
    });

    it('should render attachments list inside div-element by default', () => {
        const wrapper = mount(getComponent({}));

        expect(wrapper.find('.attachments-list').exists()).toBe(true);
        expect(wrapper.find(Carousel).exists()).toBe(false);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should render attachments list as carousel in desktop`s mobile layout', () => {
        const wrapper = mount(getComponent({ windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        expect(wrapper.find('.attachments-list').exists()).toBe(false);
        expect(wrapper.find(Carousel).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should render attachments list inside div-element on touch devices', () => {
        global.IS_TOUCH = true;
        const wrapper = mount(getComponent({ windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        expect(wrapper.find('.attachments-list').exists()).toBe(true);
        expect(wrapper.find(Carousel).exists()).toBe(false);
        expect(wrapper.render()).toMatchSnapshot();
        global.IS_TOUCH = false;
    });

    it('should call appropriate metrika functions and actions by clicking on attachments', () => {
        const wrapper = mount(getComponent({}));

        wrapper.find('.attachment:not(.attachment_has-no-preview)').first().simulate('click', {
            target: { closest: () => {} }
        });
        expect(popFnCalls(countAttachment)[0]).toEqual(['click']);
        expect(toggleSlider).toBeCalled();
    });
});
