import { Meta } from '@storybook/react';
import { IS_PRODUCTION } from '@yandex-lego/components/lib/env';

export * from './checkbox-case';
export * from './advanced-case';
export * from './checkbox-captcha-case';
export * from './advanced-captcha-case';

export default {
  title: 'Components/Captcha/Hermione',
  excludeStories: IS_PRODUCTION ? /Case/ : null,
} as Meta;
