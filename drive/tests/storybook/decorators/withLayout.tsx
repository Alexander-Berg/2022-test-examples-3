import * as React from 'react';
import { DecoratorFunction } from '@storybook/client-api';
import { StoryFnReactReturnType } from '@storybook/react/dist/ts3.9/client/preview/types';

import 'yandex-font/build/static/browser.css';
import 'shared/styles/global/reset.css';
import 'shared/styles/variables/common.css';
import 'shared/styles/variables/desktop.css';
import 'tests/storybook/styles/storybook.global.css';
import 'tests/storybook/styles/hermione.global.css';
// deprecated
import 'components/Content/index.css';

export const withLayout: DecoratorFunction<StoryFnReactReturnType> = (story) => {
    return <div id="sb-container">{story()}</div>;
};
