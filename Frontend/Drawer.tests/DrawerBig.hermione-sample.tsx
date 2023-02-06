import React from 'react';
import { render } from 'react-dom';

import { BPage } from '../../internal/components/BPage/BPage';
import { Hermione } from '../../internal/components/Hermione/Hermione';
import '../../internal/components/BPage/BPage@desktop.css';

import { configureRootTheme } from '../../Theme';
import { theme as themeDefault } from '../../Theme/presets/default';
import { Big } from '../Drawer.examples/touch-phone/big.examples';

configureRootTheme({ theme: themeDefault });

render(
    <BPage>
        <Hermione>
            <Big />
        </Hermione>
    </BPage>,
    document.getElementById('root'),
);
