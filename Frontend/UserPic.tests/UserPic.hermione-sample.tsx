import React from 'react';
import { render } from 'react-dom';
import { compose, composeU } from '@bem-react/core';

import { configureRootTheme } from '../../Theme';
import { theme as themeDefault } from '../../Theme/presets/default';

import { BPage } from '../../internal/components/BPage/BPage@desktop';
import '../../internal/components/BPage/BPage@test.css';
import { Hermione } from '../../internal/components/Hermione/Hermione';

import { UserPic as UserPicCommon } from '../UserPic@desktop';
import { withCamera } from '../_hasCamera/UserPic_hasCamera@desktop';
import { withSizeM } from '../_size/UserPic_size_m';
import { withSizeS } from '../_size/UserPic_size_s';

configureRootTheme({ theme: themeDefault });

const UserPic = compose(
    withCamera,
    composeU(withSizeM, withSizeS),
)(UserPicCommon);

render(
    <BPage>
        {['s', 'm'].map((size: any) => (
            <Hermione key={size} style={{ display: 'flex' }}>
                <Hermione element="Item" style={{ margin: 8, fontSize: 0 }}>
                    <UserPic size={size} />
                </Hermione>
                <Hermione element="Item" style={{ margin: 8, fontSize: 0 }}>
                    <UserPic avatarId="43978/351415393-30646433" size={size} />
                </Hermione>
                <Hermione element="Item" style={{ margin: 8, fontSize: 0 }}>
                    <UserPic avatarId="43978/351415393-30646433" plus size={size} />
                </Hermione>
                <Hermione element="Item" style={{ margin: 8, fontSize: 0 }}>
                    <UserPic avatarId="0/0-0" hasCamera size={size} cameraURLTarget="_blank" />
                </Hermione>
            </Hermione>
        ))}
    </BPage>,
    document.getElementById('root'),
);
