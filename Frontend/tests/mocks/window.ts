import { dataSourceCtxStub } from 'neo/tests/stubs/contexts';
import { IYa } from 'neo/types/global';
import { TFlag as TNeoFlag } from 'neo/experiments/flags';
import { TFlagValue } from 'neo/types/flags';

export function mockWindowFlags<
  TFlag extends TNeoFlag = TNeoFlag
>(flags: Partial<Record<TFlag, TFlagValue>> = {}) {
  const windowYa: IYa = {
    Neo: {
      reqid: '__reqid__',
      dataSource: dataSourceCtxStub,
      flags,
    },
  };

  window.Ya = windowYa;
}
