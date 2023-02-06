import * as React from 'react';
import { fireEvent, render } from '@testing-library/react';

let testIsTouch = false;

jest.mock('../../utils', () => ({
    device: {
        get isTouch() {
            return testIsTouch;
        },
    },
    attachRef: () => () => ({}),
}));

// eslint-disable-next-line import/first
import { Popup } from '..';

describe('Popup', () => {
    let onBeforeClose: jest.Mock;

    beforeEach(() => {
        testIsTouch = false;
        jest.useFakeTimers();
        onBeforeClose = jest.fn();
    });

    afterEach(() => {
        onBeforeClose.mockRestore();
        jest.useRealTimers();
    });

    it('should listen resize event on mounting and stop listening after unmounting', () => {
        const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
        const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');

        const wrapper = render(
            <Popup type="dialog" onBeforeClose={onBeforeClose} />,
        );

        expect(addEventListenerSpy).toBeCalledWith('resize', expect.anything(), true);

        wrapper.unmount();

        expect(removeEventListenerSpy).toBeCalledWith('resize', expect.anything(), true);

        addEventListenerSpy.mockRestore();
        removeEventListenerSpy.mockRestore();
    });

    describe('should call "onBeforeClose" during resizing', () => {
        const dispatchResize = () => {
            window.dispatchEvent(new Event('resize'));
        };

        it('on mobile', () => {
            testIsTouch = true;

            const wrapper = render(
                <Popup type="dialog" visible={false} onBeforeClose={onBeforeClose} />,
            );

            dispatchResize();

            expect(onBeforeClose).not.toBeCalled();

            wrapper.rerender(
                <Popup type="menu" visible={false} onBeforeClose={onBeforeClose} />,
            );

            dispatchResize();

            expect(onBeforeClose).not.toBeCalled();

            wrapper.rerender(
                <Popup type="menu" visible onBeforeClose={onBeforeClose} />,
            );

            dispatchResize();

            expect(onBeforeClose).not.toBeCalled();
        });

        it('on Desktop', () => {
            const wrapper = render(
                <Popup type="dialog" visible={false} onBeforeClose={onBeforeClose} />,
            );

            dispatchResize();

            expect(onBeforeClose).not.toBeCalled();

            wrapper.rerender(
                <Popup type="menu" visible={false} onBeforeClose={onBeforeClose} />,
            );

            dispatchResize();

            expect(onBeforeClose).not.toBeCalled();

            wrapper.rerender(
                <Popup type="menu" visible onBeforeClose={onBeforeClose} />,
            );

            dispatchResize();

            expect(onBeforeClose).toBeCalled();
        });
    });

    describe('should call "onBeforeClose" during change orientation', () => {
        const dispatchOrientationChange = () => {
            window.dispatchEvent(new Event('orientationchange'));
        };

        it('on mobile', () => {
            testIsTouch = true;

            const wrapper = render(
                <Popup type="dialog" visible={false} onBeforeClose={onBeforeClose} />,
            );

            dispatchOrientationChange();

            expect(onBeforeClose).not.toBeCalled();

            wrapper.rerender(
                <Popup type="menu" visible={false} onBeforeClose={onBeforeClose} />,
            );

            dispatchOrientationChange();

            expect(onBeforeClose).not.toBeCalled();

            wrapper.rerender(
                <Popup type="menu" visible onBeforeClose={onBeforeClose} />,
            );

            dispatchOrientationChange();

            expect(onBeforeClose).toBeCalled();
        });

        it('on Desktop', () => {
            const wrapper = render(
                <Popup type="dialog" visible={false} onBeforeClose={onBeforeClose} />,
            );

            dispatchOrientationChange();

            expect(onBeforeClose).not.toBeCalled();

            wrapper.rerender(
                <Popup type="menu" visible={false} onBeforeClose={onBeforeClose} />,
            );

            dispatchOrientationChange();

            expect(onBeforeClose).not.toBeCalled();

            wrapper.rerender(
                <Popup type="menu" visible onBeforeClose={onBeforeClose} />,
            );

            dispatchOrientationChange();

            expect(onBeforeClose).toBeCalled();
        });
    });

    it('should call "onBeforeClose" on clicking outside', () => {
        const wrapper = render(
            <Popup type="dialog" visible onBeforeClose={onBeforeClose} />,
        );

        fireEvent.mouseDown(wrapper.baseElement.querySelector('.ui-popup__content') as Element);

        expect(onBeforeClose).not.toBeCalled();

        fireEvent.mouseDown(wrapper.baseElement.children[0].children[0]);

        expect(onBeforeClose).toBeCalled();
    });

    it('should call "onClose" when animation is completed', async () => {
        const onClose = jest.fn();

        const wrapper = render(
            <Popup
                type="dialog"
                visible
                onBeforeClose={onBeforeClose}
                onClose={onClose}
            >
                <h1>
                    Hello
                </h1>
            </Popup>,
        );

        fireEvent.transitionEnd(wrapper.baseElement.children[0].children[0]);

        expect(onBeforeClose).not.toBeCalled();

        wrapper.rerender(
            <Popup
                type="dialog"
                visible={false}
                onBeforeClose={onBeforeClose}
                onClose={onClose}
            >
                <h1>
                    Hello
                </h1>
            </Popup>,
        );

        jest.runTimersToTime(100);

        expect(onClose).toBeCalled();
    });
});
