import * as React from 'react';
import { render } from 'react-dom';
import { fireEvent, waitFor } from '@testing-library/react';

import { useKeyboardMenu } from 'shared/hooks/useKeyboardMenu/useKeyboardMenu';

const Menu: React.FC<{ callback: () => {}; disabled?: boolean }> = function Menu({ callback, disabled }) {
    const ref = React.createRef<HTMLDivElement>();

    useKeyboardMenu(callback, disabled);

    React.useEffect(() => {
        if (ref && ref.current) {
            fireEvent.keyDown(document.body, {
                key: 'Escape',
                code: 'Escape',
                keyCode: 27,
                charCode: 27,
            });
        }
    }, [ref]);

    return <div ref={ref} />;
};

describe('useKeyboardMenu', function () {
    it("shouldn't work with disabled option", async function () {
        const callback = jest.fn();
        const root = document.createElement('div');

        document.body.appendChild(root);

        render(
            <Menu
                callback={callback}
                disabled
            />,
            root,
        );

        await waitFor(() => expect(callback).toBeCalledTimes(0));
    });

    it('should work with Escape', async function () {
        const callback = jest.fn();
        const root = document.createElement('div');

        document.body.appendChild(root);

        render(<Menu callback={callback} />, root);

        await waitFor(() => expect(callback).toBeCalled());
    });
});
