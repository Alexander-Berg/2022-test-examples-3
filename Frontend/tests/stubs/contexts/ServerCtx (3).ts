import { IServerCtx } from 'neo/types/contexts/IServerCtx';
import { ICtxType } from 'neo/types/report-renderer/ICtxRR';
import { serverCtxStub as serverCtxStubNeo } from 'neo/tests/stubs/contexts';

export function getServerCtxStub(
  args?: {
    findLastItemArgs?: Record<string, string>,
    specialArgs?: Record<string, object>,
    additionalItemMap?: Record<string, object>,
  },
): IServerCtx {
  const itemMap = args?.additionalItemMap ?? {};

  return {
    neo: {
      ...serverCtxStubNeo.neo,
      ctxRR: {
        findLastItem: <T extends ICtxType>(type: T['type']) => {
          return itemMap[args?.findLastItemArgs?.[`${type}`] || `${type}`] as T;
        },
        getItems: <T extends ICtxType>(type: T['type']) => {
          return itemMap[args?.findLastItemArgs?.[type] || type] as T[] || [];
        },
        findFirstItem: () => undefined,
        findFirstProtobufItem: () => undefined,
        findLastProtobufItem: () => undefined,
        setState: () => {},
        setResponseHeader: () => {},
        setResponseStatus: () => {},
        getProtobufItems: () => [],
        getState: <T>(_: string) => ({}) as T,
        addItem: () => {},
        appendResponseHeader: () => {},
      },
      flags: {},
      ...(args?.specialArgs?.neo || {}),
    },
  };
}
