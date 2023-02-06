import React from 'react';
import { render } from 'react-dom';

import { parseQueryString } from '@yandex-lego/components/internal/utils/parseQueryString';
import { ThemeProvider } from '@yandex-lego/components/internal/components/ThemeProvider';

import { LegacyHermioneCase } from './scenarios/legacy';
import { PinsHermioneCase } from './scenarios/pins';

const { scenario, tokens, ...props } = parseQueryString(window.location.search);

function getHermioneCase(scenario: string, props: any) {
    switch (scenario) {
        case 'legacy':
            return <LegacyHermioneCase {...props} />;
        case 'pins':
            return <PinsHermioneCase {...props} />;
        default:
            return <div>{scenario} not found</div>;
    }
}

render(
    <ThemeProvider theme={tokens}>{getHermioneCase(scenario, props)}</ThemeProvider>,
    document.getElementById('root'),
);
