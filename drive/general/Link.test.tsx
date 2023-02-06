import * as React from 'react';
import { render } from '@testing-library/react';

import { Link } from 'shared/ui/Link/Link';

describe('Link', () => {
    it('should render link with href attribute', async () => {
        const { container } = render(<Link href="https://yandex.ru">text</Link>);

        expect(container).toMatchSnapshot();
    });

    it('should render link without href attribute', async () => {
        const { container } = render(<Link>text</Link>);

        expect(container).toMatchSnapshot();
    });

    it('should render disabled link', async () => {
        const { container } = render(<Link disabled>text</Link>);

        expect(container).toMatchSnapshot();
    });
});
