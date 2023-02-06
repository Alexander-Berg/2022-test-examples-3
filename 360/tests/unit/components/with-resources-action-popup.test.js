import '../noscript';
import React from 'react';
import { Provider as ReduxProvider } from 'react-redux';
import { mount } from 'enzyme';

import getStore from '../../../components/redux/store';
import withResourcesActionsPopup from '../../../components/redux/components/with-resources-actions-popup';

const Dummy = () => (<div className="dummy" />);

const getHOC = (options = {}) => withResourcesActionsPopup(Dummy, options);
const getConnectedComponent = (Component, store, props = {}) => (
    <ReduxProvider store={store}>
        <Component {...props} />
    </ReduxProvider>
);

const getProps = () => ({
    resources: [],
    setSelection: jest.fn(),
    setHighlighted: jest.fn(),
    selected: [],
    highlighted: [],
    metrikaTail: ['listing']
});

const getWithResourcesActionsPopupWrapper = (wrapper) => wrapper.childAt(0).childAt(0);
const getWithResourcesActionsPopupInstance = (wrapper) => getWithResourcesActionsPopupWrapper(wrapper).instance();

describe('WithResourcesActionsPopup', () => {
    const addEventListenerSpy = jest.spyOn(window, 'addEventListener');

    beforeAll(() => {
        const div = document.createElement('div');
        window.domNode = div;
        document.body.appendChild(div);
    });

    afterAll(() => {
        delete window.domNode;
    });

    describe('closing on scroll', () => {
        it('should subscribe to window scroll/resize events', () => {
            const wrapper = mount(getConnectedComponent(getHOC({ closeOnWindowScroll: true, closeOnWindowResize: true }), getStore(), getProps()));
            const WithResourcesActionsPopupInstance = getWithResourcesActionsPopupInstance(wrapper);

            wrapper.unmount();
            wrapper.mount();

            expect(addEventListenerSpy).toHaveBeenCalledWith('scroll', WithResourcesActionsPopupInstance._closeContextMenu);
            expect(addEventListenerSpy).toHaveBeenCalledWith('resize', WithResourcesActionsPopupInstance._closeContextMenu);
        });

        it('should close context menu on window scroll/resize', () => {
            const wrapper = mount(getConnectedComponent(getHOC(), getStore(), getProps()));

            getWithResourcesActionsPopupInstance(wrapper).setState({ position: { left: 0, top: 0 } });

            const onCloseSpy = jest.spyOn(getWithResourcesActionsPopupInstance(wrapper), '_onClose');

            getWithResourcesActionsPopupInstance(wrapper)._closeContextMenu();

            expect(onCloseSpy).toHaveBeenCalled();
            expect(getWithResourcesActionsPopupWrapper(wrapper).state('position')).toEqual(null);
        });

        it('should close context menu on a scrolling element scroll event', () => {
            const wrapper = mount(
                getConnectedComponent(getHOC({ closeOnScrollElementSelector: '.dummy' }), getStore(), getProps()), {
                    attachTo: window.domNode
                }
            );

            const onCloseSpy = jest.spyOn(getWithResourcesActionsPopupInstance(wrapper), '_onClose');
            getWithResourcesActionsPopupInstance(wrapper).setState({ position: { left: 0, top: 0 } });

            const event = new Event('scroll');
            getWithResourcesActionsPopupInstance(wrapper)._scrollingElement.dispatchEvent(event);

            expect(onCloseSpy).toHaveBeenCalled();

            expect(getWithResourcesActionsPopupWrapper(wrapper).state('position')).toEqual(null);

            // should not call _onClose if context menu is closed
            onCloseSpy.mockClear();
            getWithResourcesActionsPopupInstance(wrapper)._scrollingElement.dispatchEvent(event);
            expect(onCloseSpy).not.toHaveBeenCalled();
        });
    });
});
