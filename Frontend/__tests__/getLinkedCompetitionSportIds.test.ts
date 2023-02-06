import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { getLinkedCompetitionSportIds } from '../getLinkedCompetitionSportIds';

describe('getLinkedCompetitionSportIds', () => {
  it('Возвращает пустое значение айдишника при передаче пустой строки', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxnerpa_sport_competition-enable': '',
          },
        },
      },
    });
    const name = getLinkedCompetitionSportIds(
      serverCtx,
    );
    expect(name).toEqual([]);
  });
  it('Возвращает айдишники из массива видов спорта', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxnerpa_sport_competition-enable': '582,113',
          },
        },
      },
    });
    const name = getLinkedCompetitionSportIds(
      serverCtx,
    );
    expect(name).toEqual([582, 113]);
  });
  it('Возвращает массив видов спорта, если передаем флаг с не существующим значением вида спорта', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxnerpa_sport_competition-enable': '620,1055,-1',
          },
        },
      },
    });
    const name = getLinkedCompetitionSportIds(
      serverCtx,
    );
    expect(name).toEqual([620]);
  });
  it('Возвращает пустой массив данных, если передаем не строчные данные', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxnerpa_sport_competition-enable': 411664654,
          },
        },
      },
    });
    const name = getLinkedCompetitionSportIds(
      serverCtx,
    );
    expect(name).toEqual([]);
  });
});
