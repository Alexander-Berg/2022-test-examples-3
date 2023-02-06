import React from 'react';
import { render } from 'react-dom';

import { ThemeProvider } from '../../internal/components/ThemeProvider';
import { Showcase } from '../__examples__/showcase';

render(
    <ThemeProvider>
        <Showcase />
    </ThemeProvider>,
    document.getElementById('root'),
);
