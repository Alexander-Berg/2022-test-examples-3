import HotKeyManager from '../HotKeyManager';
import { Callback } from '../types';

export const noop = () => { };

export interface ListenersStack {
    listeners: { [key: string]: Callback[] }; // #define private public
    subscribe: () => void;
    unsubscribe: () => void;
    dispose: () => void;
}
export const extractListenersStacks = () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const ls = (HotKeyManager as any).eventListeners;
    return Object.keys(ls).map((key) => ls[key]) as ListenersStack[];
};

export interface EmulateEventOptions {
    repeat?: boolean;
    ctrl?: boolean;
    shift?: boolean;
    alt?: boolean;
    meta?: boolean;
}
export const emulateEvent = (eventName: string, code: string, options: EmulateEventOptions = {}) => {
    return document.dispatchEvent(new KeyboardEvent(eventName, {
        code,
        repeat: options.repeat || false,
        cancelable: true,

        ctrlKey: options.ctrl,
        shiftKey: options.shift,
        altKey: options.alt,
        metaKey: options.meta,
    }));
};

export const createDescriptor = ({ key = 'Enter', ctrl = false, shift = false, alt = false, meta = false } = {}) => ({
    key,
    ctrl,
    shift,
    alt,
    meta,
});

export const withMutedConsoleError = (fn: Function): void => {
    const mock = jest.spyOn(console, 'error').mockImplementation(() => {});
    fn();
    mock.mockRestore();
};

export const expectEqualCallCount = (f1: Function, f2: Function): void => {
    expect(f1).toBeCalledTimes((f2 as jest.Mock).mock.calls.length);
};
