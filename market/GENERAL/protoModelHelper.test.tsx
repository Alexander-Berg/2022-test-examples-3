import { ParameterValue } from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { replaceValues } from 'src/shared/helpers/protoModelHelper';

it('replace pictures', () => {
  const values: ParameterValue[] = [
    {
      param_id: 1,
      xsl_name: 'xslName_1',
      bool_value: false,
    },
    {
      param_id: 2,
      xsl_name: 'xslName_2',
      numeric_value: '13.5',
    },
    {
      param_id: 2,
      xsl_name: 'xslName_2',
      numeric_value: '42',
    },
    {
      param_id: 3,
      xsl_name: 'xslName_3',
      numeric_value: '17',
    },
  ];
  const toReplace: ParameterValue[] = [
    {
      param_id: 3,
      xsl_name: 'xslName_3',
      numeric_value: '30003',
    },
    {
      param_id: 2,
      xsl_name: 'xslName_2',
      numeric_value: '20002',
    },
  ];
  expect(replaceValues(values, ...toReplace)).toEqual([
    {
      param_id: 1,
      xsl_name: 'xslName_1',
      bool_value: false,
    },
    {
      param_id: 3,
      xsl_name: 'xslName_3',
      numeric_value: '30003',
    },
    {
      param_id: 2,
      xsl_name: 'xslName_2',
      numeric_value: '20002',
    },
  ]);
});
