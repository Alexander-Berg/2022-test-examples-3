import React from 'react';
import { render } from 'react-dom';

import { BPage } from '../../internal/components/BPage/BPage';
import { Hermione } from '../../internal/components/Hermione/Hermione';

import { configureRootTheme } from '../../Theme';
import { theme as themeDefault } from '../../Theme/presets/default';

import { Text } from '../desktop/bundle';

import '../../internal/components/BPage/BPage@desktop.css';
import { typographyValues } from '../__examples__/assets';

import 'yandex-font/build/browser.css';

configureRootTheme({ theme: themeDefault });

render(
    <BPage>
        <Hermione>
            {typographyValues.map((typography, idx) => (
                <Text as="div" key={idx} weight="light" typography={typography}>
                    Миссия Яндекса — помогать людям решать задачи и достигать своих целей в жизни.
                </Text>
            ))}
        </Hermione>
    </BPage>,
    document.getElementById('root'),
);
