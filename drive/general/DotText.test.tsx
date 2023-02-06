import * as React from 'react';
import { render } from '@testing-library/react';

import { DotText } from 'shared/ui/DotText/DotText';

describe('DotText', () => {
    it('should render element', async () => {
        const { container } = render(
            <DotText items={[null, '1', '', '2', 'text', <span key="span">3</span>, undefined]} />,
        );

        expect(container).toMatchSnapshot();
    });

    it('should render with empty array', async () => {
        const { container } = render(<DotText items={[]} />);

        expect(container).toMatchSnapshot();
    });
});
