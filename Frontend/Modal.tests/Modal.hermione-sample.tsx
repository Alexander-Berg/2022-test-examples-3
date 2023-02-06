import React from 'react';
import { render } from 'react-dom';
import { parseQueryString } from '@yandex-lego/components/internal/utils/parseQueryString';

import '@yandex-lego/components/internal/components/BPage/BPage@test.css';
import './Hermione.components/BPage/BPage.css';

import { SimpleHermioneCase } from './scenarios/simple';
import { ScrollBarHermioneCase } from './scenarios/scrollbar';
import { VisibilityHermioneCase } from './scenarios/visibility';

const { scenario, ...props } = parseQueryString(window.location.search);

function getHermioneCase(scenario: string, props: any) {
    switch (scenario) {
        case 'simple':
            return <SimpleHermioneCase {...props} />;

        case 'scrollbar':
            return <ScrollBarHermioneCase {...props} />;

        case 'visibility':
            return <VisibilityHermioneCase {...props} />;

        default:
            return <div>{scenario} not found</div>;
    }
}

render(getHermioneCase(scenario, props), document.getElementById('root'));
