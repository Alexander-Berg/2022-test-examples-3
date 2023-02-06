import { getSmartDateTime } from 'news/lib/helpers/getSmartDateTime';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';

const serverCtx = getServerCtxStub();
let mock: jest.SpyInstance;

describe('getSmartDateTime', () => {
  beforeAll(() => {
    mock = jest.spyOn(Date, 'now');
    mock.mockImplementation(() => 1590688157074);
  });

  afterAll(() => {
    mock.mockRestore();
  });

  it('today', () => {
    const timestamp = Number(new Date('2020-05-28T15:26:28')) / 1000;
    const date = getSmartDateTime(serverCtx, timestamp);

    expect(date).toBe('15:26');
  });

  it('yesterday', () => {
    const timestamp = Number(new Date('2020-05-27T09:43:12')) / 1000;
    const date = getSmartDateTime(serverCtx, timestamp);

    expect(date).toBe('вчера в 09:43');
  });

  it('this year', () => {
    const timestamp = Number(new Date('2020-01-01T18:56:19')) / 1000;
    const date = getSmartDateTime(serverCtx, timestamp);

    expect(date).toBe('1 января в 18:56');
  });

  it('another time', () => {
    const timestamp = Number(new Date('2015-05-28T13:05:48')) / 1000;
    const date = getSmartDateTime(serverCtx, timestamp);

    expect(date).toBe('28.05.2015 в 13:05');
  });
});
