import React from 'react';
import { mount } from 'enzyme';
import CreateNoteButton from '../../../../../src/components/create-note-button';

const mockedShowTooManyNotesNotification = jest.fn();
const mockedOnCreateNew = jest.fn();
const getComponent = ({
    blockNoteSelection = false,
    isNotesLimitExceeded = false,
    isErrorNotificationShown = false,
    offline = false,
    isAndroidYandexBrowser = false
}) => (
    <CreateNoteButton
        blockNoteSelection={blockNoteSelection}
        isNotesLimitExceeded={isNotesLimitExceeded}
        isErrorNotificationShown={isErrorNotificationShown}
        offline={offline}
        showTooManyNotesNotification={mockedShowTooManyNotesNotification}
        isAndroidYandexBrowser={isAndroidYandexBrowser}
        onCreateNew={mockedOnCreateNew}
    />
);

const getLegoComponent = (wrapper, tagName, className) => wrapper
    .find(tagName)
    .filterWhere((component) => (
        new RegExp(className).test(component.props().cls || component.props().className))
    );

describe('src/components/create-note-button', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('on touch devices', () => {
        const originalIsTouch = global.IS_TOUCH;

        beforeAll(() => {
            global.IS_TOUCH = true;
        });

        afterAll(() => {
            global.IS_TOUCH = originalIsTouch;
        });

        it('should not render if a touch device is in offline', () => {
            const wrapper = mount(getComponent({ offline: true }));

            expect(wrapper.isEmptyRender()).toBe(true);
        });

        it('should not render on touch devices if note selection is blocked', () => {
            const wrapper = mount(getComponent({ blockNoteSelection: true }));

            expect(wrapper.isEmptyRender()).toBe(true);
        });

        it('should not render on touch devices if an error notification is shown', () => {
            const wrapper = mount(getComponent({ isErrorNotificationShown: true }));

            expect(wrapper.isEmptyRender()).toBe(true);
        });

        it('should notify about `too many notes` if the button is clicked by a user with maximum number of notes on touch devices', () => {
            const wrapper = mount(getComponent({ isNotesLimitExceeded: true }));

            wrapper.simulate('click');
            expect(mockedShowTooManyNotesNotification).toBeCalled();
            expect(mockedOnCreateNew).not.toBeCalled();
            expect(wrapper.render()).toMatchSnapshot();
        });

        it('should create note when the button is clicked on touch devices', () => {
            const wrapper = mount(getComponent({}));

            wrapper.simulate('click');
            expect(mockedShowTooManyNotesNotification).not.toBeCalled();
            expect(mockedOnCreateNew).toBeCalled();
            expect(wrapper.render()).toMatchSnapshot();
        });

        it('should add special class modificator if the app is opened in mobile yandex-browser on android device', () => {
            const wrapper = mount(getComponent({ isAndroidYandexBrowser: true }));

            expect(wrapper.render()).toMatchSnapshot();
        });
    });

    describe('on desktop', () => {
        it('should have tooltip and should not be clickable if max notes count reached', () => {
            const wrapper = mount(getComponent({ isNotesLimitExceeded: true }));
            const hoverTooltip = getLegoComponent(
                wrapper,
                'ufo-rocks_hover-tooltip',
                'create-note-button__hover-tooltip'
            );

            expect(hoverTooltip.exists()).toBe(true);
            wrapper.simulate('click');
            expect(mockedOnCreateNew).not.toBeCalled();
            expect(wrapper.render()).toMatchSnapshot();
        });

        it('should not be clickable if note selection is blocked', () => {
            const wrapper = mount(getComponent({ blockNoteSelection: true }));

            wrapper.simulate('click');
            expect(mockedOnCreateNew).not.toBeCalled();
        });

        it('should create note on button click', () => {
            const wrapper = mount(getComponent({}));

            wrapper.simulate('click');
            expect(mockedOnCreateNew).toBeCalled();
            expect(wrapper.render()).toMatchSnapshot();
        });
    });
});
