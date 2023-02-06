// @ts-ignore
global.FLAGS = {
    HOT_KEY_COMBINATIONS: true,
};
import * as React from 'react';
import { render, screen } from '@testing-library/react';

import HotKey from '../HotKey';
import HotKeyManager from '../HotKeyManager';
import { emulateEvent, expectEqualCallCount, extractListenersStacks, ListenersStack, noop } from './utils';

describe('HotKey', () => {
    const mocks: jest.SpyInstance[] = [];
    const stacks: ListenersStack[] = extractListenersStacks();

    beforeEach(() => {
        mocks.push(jest.spyOn(HotKeyManager, 'subscribe'));

        stacks.forEach((stack) => {
            mocks.push(jest.spyOn(stack, 'subscribe'));
            mocks.push(jest.spyOn(stack, 'unsubscribe'));
        });
    });

    afterEach(() => {
        mocks.forEach((mock) => mock.mockRestore());
    });

    it('рендерит null', () => {
        const wrapper = render((
            <HotKey keyCode="Enter" listener={noop} />
        ), {
            wrapper: ({ children }) => <div data-testid="renderNull">{children}</div>,
        });
        expect(screen.getByTestId('renderNull')).toBeEmptyDOMElement();
        wrapper.unmount();
    });

    it('после отмонтирования отписывается от события', () => {
        const wrapper = render((
            <HotKey keyCode="Enter" listener={noop} />
        ));
        wrapper.unmount();
        expect(HotKeyManager.subscribe).toBeCalledTimes(1);
        stacks.forEach((stack) => {
            expectEqualCallCount(stack.subscribe, stack.unsubscribe);
        });
    });

    it('вызывает preventDefault, если включена соответствующая опция', () => {
        const wrapper = render((
            <HotKey keyCode="Enter" listener={noop} preventDefault />
        ));

        const defaultPrevented = !emulateEvent('keyup', 'Enter');
        expect(defaultPrevented).toBe(true);

        wrapper.unmount();
    });

    it('не вызывает listener для повторных событий, если включена соответствующая опция', () => {
        const listener = jest.fn();

        const wrapper = render((
            <HotKey keyCode="Enter" eventName="keydown" listener={listener} noRepeats />
        ));

        emulateEvent('keydown', 'Enter');
        for (let i = 0; i < 2; i++) {
            emulateEvent('keydown', 'Enter', { repeat: true });
        }

        expect(listener).toBeCalledTimes(1);

        wrapper.unmount();
    });

    it('сохраняет порядок регистрации колбеков', () => {
        const l1 = jest.fn();
        const l2 = jest.fn();
        const l3 = jest.fn();

        const TestWrapper = function (props: { middleListener: boolean }) {
            return (
                <div>
                    <HotKey keyCode="Escape" listener={l1} preventDefault />
                    {props.middleListener ? (
                        <HotKey keyCode="Escape" listener={l2} preventDefault />
                    ) : null}
                    <HotKey keyCode="Escape" listener={l3} preventDefault />
                </div>
            );
        };

        const wrapper = render((<TestWrapper middleListener={false} />));
        emulateEvent('keyup', 'Escape');
        expect(l1).toBeCalledTimes(0);
        expect(l2).toBeCalledTimes(0);
        expect(l3).toBeCalledTimes(1);

        wrapper.rerender(<TestWrapper middleListener />);
        emulateEvent('keyup', 'Escape');
        expect(l1).toBeCalledTimes(0);
        expect(l2).toBeCalledTimes(1);
        expect(l3).toBeCalledTimes(1);

        wrapper.rerender(<TestWrapper middleListener={false} />);
        emulateEvent('keyup', 'Escape');
        expect(l1).toBeCalledTimes(0);
        expect(l2).toBeCalledTimes(1);
        expect(l3).toBeCalledTimes(2);

        wrapper.unmount();
    });
});
