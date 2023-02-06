import React from 'react';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';
import getStore from '../../../../../src/store';
import { STATES, DESKTOP_LAYOUT_THRESHOLD } from '../../../../../src/consts';

const mockedEditor = {
    getSnippet: jest.fn(),
    focus: jest.fn()
};

jest.mock('@ps-int/ufo-rocks/lib/components/with-debounced-change',
    () => (WrappedComponent) => WrappedComponent);
jest.mock('../../../../../src/components/editor', () => ({ setRef }) => {
    setRef(mockedEditor);
    return null;
});
jest.mock('@ps-int/ufo-rocks/lib/components/groupable-buttons', () => () => null);
jest.mock('../../../../../src/store/actions', () => ({
    fetchNoteContent: jest.fn(() => ({ type: '' })),
    closeDialog: jest.fn(() => ({ type: '' })),
    deleteCurrent: jest.fn(() => ({ type: '' })),
    createNote: jest.fn(() => () => Promise.resolve()),
    updateWindowWidth: jest.fn(() => ({ type: '' }))
}));
jest.mock('../../../../../src/store/actions/init', () => ({
    initNotesList: jest.fn(() => ({ type: '' }))
}));
jest.mock('../../../../../src/store/actions/attachments', () => ({
    batchRequestAttachments: jest.fn(() => ({ type: '' })),
    deleteAttachment: jest.fn(() => ({ type: '' })),
    toggleSlider: jest.fn(() => ({ type: '' }))
}));
jest.mock('@ps-int/ufo-rocks/lib/helpers/offline');
jest.mock('../../../../../src/helpers/metrika', () => ({
    countError: jest.fn(),
    count: jest.fn()
}));

const getState = ({
    OSFamily,
    errorCode,
    diskNotesProdExperiment,
    windowWidth
}) => ({
    ua: { isSmartphone: false, OSFamily, isMobile: false, isIosSafari: false },
    user: { auth: true },
    notes: {
        current: 'note-1',
        sliderResourceId: null,
        blockNoteSelection: false,
        notes: {
            'note-1': {
                id: 'note-1',
                title: '',
                mtime: '2011-12-21T21:11:55.946Z',
                attachmentOrder: [],
                tags: {},
                content: {}
            },
            'note-2': {
                id: 'note-2',
                title: 'note two',
                mtime: '2018-12-20T14:25:14.488Z',
                attachmentOrder: [],
                tags: {},
                content: {}
            }
        },
        state: STATES.LOADED
    },
    environment: {
        experiments: {
            flags: { disk_notes_prod_experiment: diskNotesProdExperiment }
        },
        windowWidth
    },
    notifications: {
        current: { text: 'notification text' },
        items: [],
        state: ''
    },
    dialogs: {},
    errorCode
});
const getComponent = ({
    OSFamily = 'Windows',
    errorCode = null,
    diskNotesProdExperiment = false,
    windowWidth = DESKTOP_LAYOUT_THRESHOLD
}) => {
    const Notes = require('../../../../../src/components/notes').default;

    return (
        <Provider store={getStore(getState({ OSFamily, errorCode, diskNotesProdExperiment, windowWidth }))}>
            <Notes />
        </Provider>
    );
};

describe('components/notes =>', () => {
    let NotesSlider;
    let Note;
    let NotesList;
    let Error;
    let NoteDeleteConfirmationDialog;
    let AttachmentDeleteConfirmationDialog;
    let Notifications;
    let AppPromo;
    let countError;

    beforeEach(() => {
        jest.resetModules();

        const { setTankerProjectId, addTranslation } = require('react-tanker');

        setTankerProjectId('yandex_disk_web');
        addTranslation('ru', require('../../../../../i18n/loc/ru'));

        NotesSlider = require('../../../../../src/components/notes-slider').default;
        Note = require('../../../../../src/components/note').default;
        NotesList = require('../../../../../src/components/notes-list').default;
        Error = require('../../../../../src/components/error').default;
        NoteDeleteConfirmationDialog = require('../../../../../src/components/dialogs/note-delete-confirmation-dialog').default;
        AttachmentDeleteConfirmationDialog = require('../../../../../src/components/dialogs/attachment-delete-confirmation-dialog').default;
        Notifications = require('@ps-int/ufo-rocks/lib/components/notifications').default;
        AppPromo = require('../../../../../src/components/app-promo').default;
        countError = require('../../../../../src/helpers/metrika').countError;
    });

    it('should render list of notes, note, slider, notifications and dialogs by default', () => {
        const wrapper = mount(getComponent({}));

        expect(wrapper.find(NotesList).exists()).toBe(true);
        expect(wrapper.find(Note).exists()).toBe(true);
        expect(wrapper.find(NotesSlider).exists()).toBe(true);
        expect(wrapper.find(Notifications).exists()).toBe(true);
        expect(wrapper.find(NoteDeleteConfirmationDialog).exists()).toBe(true);
        expect(wrapper.find(AttachmentDeleteConfirmationDialog).exists()).toBe(true);
        expect(wrapper.find(AppPromo).exists()).toBe(false);
        expect(wrapper.find(Error).exists()).toBe(false);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should render list of notes, note, slider, notifications and note delete dialog on touch devices', () => {
        global.IS_TOUCH = true;

        const wrapper = mount(getComponent({ windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        expect(wrapper.find(NotesList).exists()).toBe(true);
        expect(wrapper.find(Note).exists()).toBe(true);
        expect(wrapper.find(NotesSlider).exists()).toBe(true);
        expect(wrapper.find(Notifications).exists()).toBe(true);
        expect(wrapper.find(NoteDeleteConfirmationDialog).exists()).toBe(true);
        expect(wrapper.find(AttachmentDeleteConfirmationDialog).exists()).toBe(false);
        expect(wrapper.find(AppPromo).exists()).toBe(false);
        expect(wrapper.find(Error).exists()).toBe(false);
        expect(wrapper.render()).toMatchSnapshot();
        global.IS_TOUCH = false;
    });

    it('should show error page when error code is received', () => {
        const wrapper = mount(getComponent({ errorCode: 500 }));

        expect(wrapper.find(Error).exists()).toBe(true);
    });

    it('should focus on editor when note creation button is clicked', () => {
        mockedEditor.focus.mockReset();

        const wrapper = mount(getComponent({}));
        const createNoteButton = wrapper
            .find('lego-components_button')
            .filterWhere((button) => /create-note-button_desktop/.test(button.props().className));

        expect(mockedEditor.focus).not.toBeCalled();
        createNoteButton.simulate('click');
        expect(mockedEditor.focus).toBeCalled();
    });

    it('should render app promo banner on android devices under experiment', () => {
        global.IS_TOUCH = true;

        const wrapper = mount(getComponent({ OSFamily: 'Android', diskNotesProdExperiment: true, windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        expect(wrapper.find(AppPromo).exists()).toBe(true);
        global.IS_TOUCH = false;
    });

    it('should call metrika function on closing error notification', () => {
        const wrapper = mount(getComponent({}));

        wrapper.find('.MessageBox-Close').simulate('click');
        expect(popFnCalls(countError)[0]).toEqual(['close notification']);
    });
});
