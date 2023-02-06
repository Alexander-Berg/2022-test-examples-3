import * as React from 'react';
import { render, screen } from '@testing-library/react';

import { createNamedHotKey, HotKeyName } from '../NamedHotKey';
import { noop, emulateEvent } from './utils';

describe('NamedHotKey', () => {
    const hotKeys = {
        TestHotkey: {
            text: null,
            descriptor: {
                key: 'Enter',
                ctrl: true,
                shift: false,
                alt: false,
                meta: false,
            },
        },
    };

    const NamedHotKey = createNamedHotKey(hotKeys);

    it('рендерит null', () => {
        const wrapper = render((
            <NamedHotKey name="TestHotkey" listener={noop} />
        ), {
            wrapper: ({ children }) => <div data-testid="test">{children}</div>,
        });
        expect(screen.getByTestId('test')).toBeEmptyDOMElement();
        wrapper.unmount();
    });

    it('вызывает слушателей для именованных комбинаций клавиш', () => {
        const combo: HotKeyName<typeof hotKeys> = 'TestHotkey';
        const hotkeyDescriptor = hotKeys[combo].descriptor;
        const listener = jest.fn();

        const wrapper = render((
            <NamedHotKey name={combo} listener={listener} />
        ));

        emulateEvent('keyup', hotkeyDescriptor.key, { ...hotkeyDescriptor });

        expect(listener).toBeCalledTimes(1);

        wrapper.unmount();
    });
});
