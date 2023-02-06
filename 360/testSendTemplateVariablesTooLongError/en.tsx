import external_0_plural_adv from '../../../tanker/dynamic/plural_adv/en';

import type { TTestSendTemplateVariablesTooLongError } from './types';

const testSendTemplateVariablesTooLongError: TTestSendTemplateVariablesTooLongError =
  function (params) {
    return external_0_plural_adv({
      count: params['count'],
      one: 'The variable is longer than ' + params['count'] + ' characters',
      some: 'The variable is longer than ' + params['count'] + ' characters',
      many: 'The variable is longer than ' + params['count'] + ' characters',
      none: 'The variable is longer than ' + params['count'] + ' characters',
    } as const);
  };

export default testSendTemplateVariablesTooLongError;
