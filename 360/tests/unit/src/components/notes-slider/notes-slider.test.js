import React from 'react';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';
import getStore from '../../../../../src/store';
import { STATES } from '../../../../../src/consts';
import { ACTIONS } from '../../../../../src/components/notes-slider';

jest.mock('../../../../../src/helpers/metrika', () => ({
    countNote: jest.fn()
}));
jest.mock('../../../../../src/store/actions', () => ({
    openDialog: jest.fn(() => ({ type: '' }))
}));
jest.mock('../../../../../src/store/actions/attachments', () => ({
    toggleSlider: jest.fn(() => ({ type: '' })),
    updateAttachment: jest.fn(() => ({ type: '' }))
}));
jest.mock('@ps-int/ufo-rocks/lib/components/groupable-buttons', () => ({ default: () => <div /> }));

const CURRENT_NOTE_ID = 'note-1';
const ATTACHMENT_ORDER = ['attachment1', 'attachment2'];
const getState = ({ sliderResourceId }) => ({
    notes: {
        current: CURRENT_NOTE_ID,
        sliderResourceId,
        notes: {
            'note-1': {
                id: 'note-1',
                attachmentOrder: ATTACHMENT_ORDER,
                attachments: {
                    attachment1: { resourceId: 'attachment1', preview: 'previewUrl', file: 'fileUrl', state: STATES.LOADED },
                    attachment2: { resourceId: 'attachment2', preview: 'previewUrl', file: 'fileUrl', state: STATES.LOADED }
                }
            }
        }
    },
    ua: { isMobile: false, isIosSafari: false }
});
const getComponent = ({ sliderResourceId = null }) => {
    const NotesSlider = require('../../../../../src/components/notes-slider').default;

    return (
        <Provider store={getStore(getState({ sliderResourceId }))}>
            <NotesSlider />
        </Provider>
    );
};

describe('components/notes =>', () => {
    let GroupableButtons;
    let Slider;
    let updateAttachment;
    let toggleSlider;
    let openDialog;
    let countNote;
    const getButtonByCls = (wrapper, cls) => wrapper
        .find('lego-components_button')
        .filterWhere((button) => new RegExp(cls).test(button.props().className));

    beforeEach(() => {
        jest.resetModules();

        const { setTankerProjectId, addTranslation } = require('react-tanker');

        setTankerProjectId('yandex_disk_web');
        addTranslation('ru', require('../../../../../i18n/loc/ru'));

        GroupableButtons = require('@ps-int/ufo-rocks/lib/components/groupable-buttons').default;
        Slider = require('@ps-int/ufo-rocks/lib/components/slider').default;
        updateAttachment = require('../../../../../src/store/actions/attachments').updateAttachment;
        toggleSlider = require('../../../../../src/store/actions/attachments').toggleSlider;
        openDialog = require('../../../../../src/store/actions').openDialog;
        countNote = require('../../../../../src/helpers/metrika').countNote;
    });

    it('should not render slider if there is no selected attachment', () => {
        const wrapper = mount(getComponent({}));

        expect(wrapper.isEmptyRender()).toBe(true);
    });

    it('should render slider with delete, show-fullsize and close buttons if attachment is selected', () => {
        const wrapper = mount(getComponent({ sliderResourceId: ATTACHMENT_ORDER[0] }));
        const actionButtons = wrapper.find(GroupableButtons);

        expect(actionButtons.exists()).toBe(true);
        expect(actionButtons.props().buttons.map(({ id }) => id)).toEqual([ACTIONS.DELETE, ACTIONS.SHOW_FULLSIZE]);
        expect(getButtonByCls(wrapper, 'notes-slider__button_close').exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should open confirmation dialog in slider if delete buttons was clicked', () => {
        const wrapper = mount(getComponent({ sliderResourceId: ATTACHMENT_ORDER[0] }));

        wrapper.find(GroupableButtons).props().onButtonClick(ACTIONS.DELETE);
        expect(popFnCalls(openDialog)[0]).toEqual(['attachmentDeleteConfirmation', { resourceId: ATTACHMENT_ORDER[0], source: 'from viewer' }]);
    });

    it('should update attachment on image error in slider', () => {
        const wrapper = mount(getComponent({ sliderResourceId: ATTACHMENT_ORDER[0] }));

        wrapper.find(Slider).props().onImageError({ resourceId: ATTACHMENT_ORDER[0] });
        expect(popFnCalls(updateAttachment)[0]).toEqual([CURRENT_NOTE_ID, ATTACHMENT_ORDER[0], { error: true }]);
    });

    it('should call metrika and an appropriate action on slider closed', () => {
        const wrapper = mount(getComponent({ sliderResourceId: ATTACHMENT_ORDER[0] }));

        getButtonByCls(wrapper, 'notes-slider__button_close').simulate('click');
        expect(popFnCalls(countNote)[0]).toEqual(['viewer', 'close']);
        expect(popFnCalls(toggleSlider)[0]).toEqual([]);
    });

    it('should call an appropriate action on slide change', () => {
        const wrapper = mount(getComponent({ sliderResourceId: ATTACHMENT_ORDER[0] }));

        getButtonByCls(wrapper, 'switch-arrow-button_right').simulate('click');
        expect(popFnCalls(toggleSlider)[0]).toEqual([ATTACHMENT_ORDER[1]]);
    });

    it('should render slider without action buttons if attachment is selected on touch devices', () => {
        global.IS_TOUCH = true;

        const wrapper = mount(getComponent({ sliderResourceId: ATTACHMENT_ORDER[0] }));

        expect(wrapper.find(GroupableButtons).exists()).toBe(false);
        expect(wrapper.find(Slider).exists()).toBe(true);
        expect(getButtonByCls(wrapper, 'notes-slider__button_close').exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
        global.IS_TOUCH = false;
    });
});
