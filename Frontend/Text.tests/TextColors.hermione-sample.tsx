import React from 'react';
import { render } from 'react-dom';

import { BPage } from '../../internal/components/BPage/BPage';
import { Hermione } from '../../internal/components/Hermione/Hermione';

import { configureRootTheme } from '../../Theme';
import { theme as themeDefault } from '../../Theme/presets/default';

import '../../internal/components/BPage/BPage@desktop.css';

import 'yandex-font/build/browser.css';
import { LinkColor } from '../__examples__/link-color';
import { ControlColor } from '../__examples__/control-color';
import { Color } from '../__examples__/color';

configureRootTheme({ theme: themeDefault });

render(
    <BPage>
        <Hermione>
            {LinkColor()}
            {ControlColor()}
            {Color()}
        </Hermione>
    </BPage>,
    document.getElementById('root'),
);
