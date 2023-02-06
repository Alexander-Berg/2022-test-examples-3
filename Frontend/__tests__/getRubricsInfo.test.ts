import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { ICtxRR } from 'neo/types/report-renderer';
import { IRubricsInfo } from 'mg/types/apphost/rubrics_info';
import { getRubricsInfo } from '../getRubricsInfo';

describe('getRubricsInfo', () => {
  it('Возвращает объект IRubricsInfo, если rubrics приходит в данных', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          ctxRR: {
            getItems: (): IRubricsInfo[] => {
              return [
                {
                  type: 'rubrics_info',
                  regions: {
                    '1': {
                      names: {
                        ru: 'Хоккей',
                      },
                      no_banner: 1,
                      alias: 'h',
                      is_special: false,
                      url: 'https://yandex.ru/sport/rubric/hockey',
                      country_id: 12,
                    },
                  },
                  rubrics: {
                    '1': {
                      names: {
                        ru: 'Хоккей',
                      },
                      no_banner: 1,
                      alias: 'h',
                      is_special: false,
                      url: 'https://yandex.ru/sport/rubric/hockey',
                      country_id: 12,
                    },
                  },
                },
                {
                  type: 'rubrics_info',
                  regions: {
                    '2': {
                      names: {
                        ru: 'Футбол',
                      },
                      no_banner: 1,
                      alias: 'f',
                      is_special: false,
                      url: 'https://yandex.ru/sport/rubric/football',
                      country_id: 14,
                    },
                  },
                  rubrics: {
                    '2': {
                      names: {
                        ru: 'Футбол',
                      },
                      no_banner: 1,
                      alias: 'f',
                      is_special: false,
                      url: 'https://yandex.ru/sport/rubric/football',
                      country_id: 14,
                    },
                  },
                },
              ];
            },
          } as unknown as ICtxRR,
        },
      },
    });

    const rubricsInfo = getRubricsInfo(serverCtx.neo.ctxRR);

    expect(rubricsInfo).toEqual({
      rubrics: {
        '1': {
          names: {
            ru: 'Хоккей',
          },
          no_banner: 1,
          alias: 'h',
          is_special: false,
          url: 'https://yandex.ru/sport/rubric/hockey',
          country_id: 12,
        },
        '2': {
          names: {
            ru: 'Футбол',
          },
          no_banner: 1,
          alias: 'f',
          is_special: false,
          url: 'https://yandex.ru/sport/rubric/football',
          country_id: 14,
        },
      },
      regions: {
        '1': {
          names: {
            ru: 'Хоккей',
          },
          no_banner: 1,
          alias: 'h',
          is_special: false,
          url: 'https://yandex.ru/sport/rubric/hockey',
          country_id: 12,
        },
        '2': {
          names: {
            ru: 'Футбол',
          },
          no_banner: 1,
          alias: 'f',
          is_special: false,
          url: 'https://yandex.ru/sport/rubric/football',
          country_id: 14,
        },
      },
    });
  });

  it('Возвращает объект IRubricsInfo, если rubrics не приходит в данных', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          ctxRR: {
            getItems: (): IRubricsInfo[] => {
              return [
                {
                  type: 'rubrics_info',
                  regions: {
                    '1': {
                      names: {
                        ru: 'Хоккей',
                      },
                      no_banner: 1,
                      alias: 'h',
                      is_special: false,
                      url: 'https://yandex.ru/sport/rubric/hockey',
                      country_id: 12,
                    },
                  },
                  rubrics: {},
                },
                {
                  type: 'rubrics_info',
                  regions: {
                    '2': {
                      names: {
                        ru: 'Футбол',
                      },
                      no_banner: 1,
                      alias: 'f',
                      is_special: false,
                      url: 'https://yandex.ru/sport/rubric/football',
                      country_id: 14,
                    },
                  },
                  rubrics: {},
                },
              ];
            },
          } as unknown as ICtxRR,
        },
      },
    });

    const rubricsInfo = getRubricsInfo(serverCtx.neo.ctxRR);

    expect(rubricsInfo).toEqual({
      rubrics: {},
      regions: {
        '1': {
          names: {
            ru: 'Хоккей',
          },
          no_banner: 1,
          alias: 'h',
          is_special: false,
          url: 'https://yandex.ru/sport/rubric/hockey',
          country_id: 12,
        },
        '2': {
          names: {
            ru: 'Футбол',
          },
          no_banner: 1,
          alias: 'f',
          is_special: false,
          url: 'https://yandex.ru/sport/rubric/football',
          country_id: 14,
        },
      },
    });
  });
});
