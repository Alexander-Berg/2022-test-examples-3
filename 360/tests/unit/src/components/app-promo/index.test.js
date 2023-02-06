import React from 'react';
jest.mock('../../../../../src/components/note', () => () => <div>Note</div>);
jest.mock('../../../../../src/components/notes-list', () => () => <div>Notes list</div>);
jest.mock('../../../../../src/components/notes-slider', () => () => <div>Notes slider</div>);
jest.mock('../../../../../src/components/dialogs/attachment-delete-confirmation-dialog', () => () => <div>Attachment delete confirmation dialog</div>);
jest.mock('../../../../../src/components/dialogs/note-delete-confirmation-dialog', () => () => <div>Note delete confirmation dialog</div>);
jest.mock('@ps-int/ufo-rocks/lib/components/notifications', () => () => <div>Notifications</div>);

import { Provider } from 'react-redux';
import { mount } from 'enzyme';
import getStore from '../../../../../src/store';
import { STATES } from '../../../../../src/consts';
import AppPromo from '../../../../../src/components/app-promo';

const getComponent = (store) => {
    const Notes = require('../../../../../src/components/notes').default;
    return (
        <Provider store={store}>
            <Notes />
        </Provider>
    );
};

const getInitialState = ({ current, OSFamily = 'Android', diskNotesProdExperiment = true }) => ({
    ua: {
        OSFamily,
        isSmartphone: true
    },
    notes: {
        state: STATES.LOADED,
        current,
        notes: {}
    },
    environment: {
        experiments: {
            flags: {
                disk_notes_prod_experiment: diskNotesProdExperiment
            }
        }
    },
    errorCode: null
});

describe('components/app-promo', () => {
    let wrapper;

    beforeAll(() => {
        global.IS_TOUCH = true;
    });

    afterAll(() => {
        global.IS_TOUCH = false;
    });

    it('should display an app promo on mobile Android devices', () => {
        wrapper = mount(getComponent(getStore(getInitialState({}))));

        const appPromo = wrapper.find(AppPromo);

        expect(appPromo.length).toBe(1);
        expect(appPromo.first().props().isHidden).toBe(false);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should not display an app promo if it is not Android', () => {
        wrapper = mount(getComponent(getStore(getInitialState({ OSFamily: 'iOS' }))));

        expect(wrapper.find(AppPromo).length).toBe(0);
    });

    it('should not be visible if a note is selected', () => {
        wrapper = mount(getComponent(getStore(getInitialState({ current: 'note-1' }))));

        const appPromo = wrapper.find(AppPromo);

        expect(appPromo.length).toBe(1);
        expect(appPromo.first().props().isHidden).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should not be visible if disk_notes_prod_experiment is not active', () => {
        wrapper = mount(getComponent(getStore(getInitialState({ current: 'note-1', diskNotesProdExperiment: false }))));

        expect(wrapper.find(AppPromo).length).toBe(0);
    });
});
