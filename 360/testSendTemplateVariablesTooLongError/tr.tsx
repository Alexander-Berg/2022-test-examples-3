import external_0_plural_adv from '../../../tanker/dynamic/plural_adv/tr';

import type { TTestSendTemplateVariablesTooLongError } from './types';

const testSendTemplateVariablesTooLongError: TTestSendTemplateVariablesTooLongError =
  function (params) {
    return external_0_plural_adv({
      count: params['count'],
      one: 'Değişken uzunluğu ' + params['count'] + ' karakterden fazla',
      some: 'Değişken uzunluğu ' + params['count'] + ' karakterden fazla',
      many: 'Değişken uzunluğu ' + params['count'] + ' karakterden fazla',
      none: 'Değişken uzunluğu ' + params['count'] + ' karakterden fazla',
    } as const);
  };

export default testSendTemplateVariablesTooLongError;
