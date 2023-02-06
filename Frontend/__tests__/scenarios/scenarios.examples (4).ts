import { Meta } from '@storybook/react';
import { IS_PRODUCTION } from '@yandex-lego/components/lib/env';

export * from './simple';

export default {
  title: 'Widgets/Signup/Hermione',
  excludeStories: IS_PRODUCTION ? /Case/ : null,
} as Meta;
