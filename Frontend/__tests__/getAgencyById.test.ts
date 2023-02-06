import { getServerCtxStub } from 'mg/tests/stubs/contexts/ServerCtx';
import { IAgenciesInfo } from 'mg/types/apphost';
import { getAgencyById } from '../getAgencyById';

function getAgenciesInfoMock(): IAgenciesInfo[] {
  return [
    {
      type: 'agencies_info',
      '2': {
        actual_name: 'АИФ',
      },
    },
  ];
}

function getServerCtx(mock?: IAgenciesInfo[]) {
  return getServerCtxStub({
    findLastItemArgs: { agencies_info: 'agencies_info' },
    additionalItemMap: { agencies_info: mock ?? getAgenciesInfoMock() },
  });
}

describe('getAgencyById', function() {
  it('Находит нужное агенство', function() {
    const serverCtx = getServerCtx();
    const agency = getAgencyById(serverCtx, 2);

    expect(agency?.actual_name).toBe('АИФ');
  });

  it('Возвращает null, если не удалось найти нужное агенство', function() {
    const serverCtx = getServerCtx();
    const agency = getAgencyById(serverCtx, 10);

    expect(agency).toBeNull();
  });
});
