import React from 'react';
import { render } from 'react-dom';

import { BPage } from '../../internal/components/BPage/BPage';
import { Hermione } from '../../internal/components/Hermione/Hermione';

import { configureRootTheme } from '../../Theme';
import { theme as themeDefault } from '../../Theme/presets/default';

import '../../internal/components/BPage/BPage@desktop.css';

import 'yandex-font/build/browser.css';

import { Default } from '../__examples__/default';

configureRootTheme({ theme: themeDefault });

render(
    <BPage>
        <Hermione>
            <Default />
        </Hermione>
    </BPage>,
    document.getElementById('root'),
);
