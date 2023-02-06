import React from 'react';
import { number, withKnobs } from '@storybook/addon-knobs';

import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { CashbackPlusCoins, CashbackMVideoCoins } from '..';
import type { TCashbackPlusProps, TCashbackMVideoProps } from '../CashbackCoins.typings';

createPlatformStories<TCashbackPlusProps>('Tests/CashbackPlusCoins', CashbackPlusCoins, stories => {
    stories
        .addDecorator(withKnobs)
        .add('plain', CashbackPlusCoins => (
            <CashbackPlusCoins amount={number('amount', 21)} />
        ));
});

createPlatformStories<TCashbackMVideoProps>('Tests/CashbackMVideoCoins', CashbackMVideoCoins, stories => {
    stories
        .addDecorator(withKnobs)
        .add('plain', CashbackMVideoCoins => (
            <CashbackMVideoCoins amount={number('amount', 21)} />
        ));
});
