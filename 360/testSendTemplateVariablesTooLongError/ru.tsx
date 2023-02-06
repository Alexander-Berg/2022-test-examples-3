import external_0_plural_adv from '../../../tanker/dynamic/plural_adv/ru';

import type { TTestSendTemplateVariablesTooLongError } from './types';

const testSendTemplateVariablesTooLongError: TTestSendTemplateVariablesTooLongError =
  function (params) {
    return external_0_plural_adv({
      count: params['count'],
      one: 'Длина переменной больше ' + params['count'] + ' символа',
      some: 'Длина переменной больше ' + params['count'] + ' символов',
      many: 'Длина переменной больше ' + params['count'] + ' символов',
      none: 'Длина переменной больше ' + params['count'] + ' символа',
    } as const);
  };

export default testSendTemplateVariablesTooLongError;
