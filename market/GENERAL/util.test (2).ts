import { QueryArgs } from '@yandex-market/typesafe-query';

import { composeUrlProps } from 'src/pages/Formalization/util';

describe('Formalization utility', () => {
  const expectWorksCorrectly = (composed: any, plain: any) => (key: string, valueToDecode: any, valueToEncode: any) => {
    expect(plain[key].type.decode(valueToDecode)).toStrictEqual(composed[key].type.decode(valueToDecode));
    expect(plain[key].type.encode(valueToEncode)).toStrictEqual(composed[key].type.encode(valueToEncode));
  };

  it('composeUrlProps', () => {
    const urlPropsComposed = composeUrlProps({
      num: Number(),
      str: String(),
      bool: Boolean(),
    });
    const urlProps = {
      num: QueryArgs.number(),
      str: QueryArgs.string(),
      bool: QueryArgs.boolean(),
    };

    const inspect = expectWorksCorrectly(urlPropsComposed, urlProps);

    inspect('num', -1, '-1');
    inspect('num', 0, '0');
    inspect('num', 1, '1');

    inspect('str', '', '');
    inspect('str', '0', '0');
    inspect('str', '-1', '-1');

    inspect('bool', true, '1');
    inspect('bool', false, '0');
    inspect('bool', 'true', '-');
  });
});
