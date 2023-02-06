import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { getLinkedTeamSportIds } from '../getLinkedTeamsSportIds';

describe('getLinkedTeamSportIds', () => {
  it('Возвращает пустое значение айдишника при передаче пустой строки', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            yxnerpa_sport_team_enable: '',
          },
        },
      },
    });
    const name = getLinkedTeamSportIds(
      serverCtx,
    );
    expect(name).toEqual([]);
  });
  it('Возвращает айдишники из массива видов спорта', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            yxnerpa_sport_team_enable: '582,113',
          },
        },
      },
    });
    const name = getLinkedTeamSportIds(
      serverCtx,
    );
    expect(name).toEqual([582, 113]);
  });
  it('Возвращает массив видов спорта, если передаем флаг с не существующим значением вида спорта', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            yxnerpa_sport_team_enable: '620,1055,-1',
          },
        },
      },
    });
    const name = getLinkedTeamSportIds(
      serverCtx,
    );
    expect(name).toEqual([620]);
  });
  it('Возвращает пустой массив данных, если передаем не строчные данные', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            yxnerpa_sport_team_enable: 411664654,
          },
        },
      },
    });
    const name = getLinkedTeamSportIds(
      serverCtx,
    );
    expect(name).toEqual([]);
  });
});
