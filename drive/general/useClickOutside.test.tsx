import * as React from 'react';
import { render } from 'react-dom';
import { fireEvent, waitFor } from '@testing-library/react';

import { useClickOutside } from 'shared/hooks/useClickOutside/useClickOutside';

describe('useClickOutside', function () {
    it('should work correct', async function () {
        const callback = jest.fn();
        const root = document.createElement('div');
        const img = document.createElement('img');

        document.body.appendChild(root);
        document.body.appendChild(img);

        const Btn: React.FC = function Btn() {
            const ref = React.createRef<HTMLButtonElement>();

            useClickOutside(ref, callback);

            React.useEffect(() => {
                if (ref && ref.current) {
                    fireEvent.mouseDown(document.body);
                    fireEvent.mouseDown(img);
                }
            }, [ref]);

            return <button ref={ref} />;
        };

        render(<Btn />, root);

        await waitFor(() => expect(callback).toBeCalled());
    });
});
