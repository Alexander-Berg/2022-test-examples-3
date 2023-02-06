import { Meta } from '@storybook/react';
import { IS_PRODUCTION } from '@yandex-lego/components/lib/env';

export * from './link';
export * from './action';
export * from './serp';

export default {
  title: 'Widgets/Login/Hermione',
  excludeStories: IS_PRODUCTION ? /Case/ : null,
} as Meta;
