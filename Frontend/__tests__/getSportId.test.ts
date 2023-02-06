import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { ICtxRR } from 'neo/types/report-renderer';
import { getSportId } from '../getSportId';

describe('getSportId', () => {
  it('Возвращает event.sport._id', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          ctxRR: {
            findLastItem: (): {} => {
              return {
                sport: { _id: '123' },
              };
            },
          } as unknown as ICtxRR,
        },
      },
    });

    const sportId = getSportId(serverCtx);

    expect(sportId).toBe('123');
  });

  it('Должен вернуть undefined, если айди не пришел в данных', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          ctxRR: {
            findLastItem: () => undefined,
          } as unknown as ICtxRR,
        },
      },
    });

    const sportId = getSportId(serverCtx);

    expect(sportId).toBe(undefined);
  });
});
