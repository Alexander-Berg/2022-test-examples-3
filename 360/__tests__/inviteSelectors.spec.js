import {
  getEnabledOffice,
  getInviteShowDate,
  getResourcesFilter,
  getOffice,
  getFormIntervals,
  getIntervals,
  getRestrictions,
  isInvite,
  getOfficeIdFromQuery
} from '../inviteSelectors';

describe('inviteSelectors', () => {
  test('getEnabledOffice', () => {
    const state = {
      invite: {
        interface: {
          officeId: 777
        }
      }
    };

    expect(getEnabledOffice(state)).toEqual(777);
  });

  test('getInviteShowDate', () => {
    const state = {
      invite: {
        interface: {
          showDate: 777
        }
      }
    };

    expect(getInviteShowDate(state)).toEqual(777);
  });

  test('getResourcesFilter', () => {
    const state = {
      invite: {
        filter: {
          video: true,
          small: true,
          omg: false
        }
      }
    };

    expect(getResourcesFilter(state)).toEqual('video,small');
  });

  test('getOffice', () => {
    const resources = {
      offices: {
        777: {}
      }
    };

    expect(getOffice.resultFunc(resources, 777)).toEqual({});
  });

  describe('getRestrictions', () => {
    test('должен возвращать ограничения бронирования для заданной даты', () => {
      const restrictions1 = 'restrictions1';
      const restrictions2 = 'restrictions2';
      const date1 = Number(new Date(2020, 5, 10));
      const date2 = Number(new Date(2020, 5, 11));
      const resources = {
        restrictions: {
          [date1]: restrictions1,
          [date2]: restrictions2
        }
      };

      expect(getRestrictions.resultFunc(resources, date1)).toEqual(restrictions1);
    });
  });

  describe('getFormIntervals', () => {
    test('должен возвращать интервалы для переговорок из формы', () => {
      const start = Number(new Date(2020, 5, 10, 15, 30));
      const end = Number(new Date(2020, 5, 10, 16, 30));
      const resource1Email = 'pewpew@ya.ru';
      const resource2Email = 'pewpewpew@ya.ru';
      const formValues = {
        start,
        end,
        resources: [{resource: {email: resource1Email}}, {resource: {email: resource2Email}}]
      };
      const expectedIntervalsMap = {
        [resource1Email]: [
          {
            eventId: 'draft',
            eventIds: ['draft'],
            clippedStart: start,
            clippedEnd: end,
            start: start,
            end: end,
            isFirstOfGroup: true,
            isLastOfGroup: true,
            isReservation: true
          }
        ],
        [resource2Email]: [
          {
            eventId: 'draft',
            eventIds: ['draft'],
            clippedStart: start,
            clippedEnd: end,
            start: start,
            end: end,
            isFirstOfGroup: true,
            isLastOfGroup: true,
            isReservation: true
          }
        ]
      };

      expect(getFormIntervals.resultFunc(formValues)).toEqual(expectedIntervalsMap);
    });

    test('должен возвращать null, если нет нужной формы', () => {
      expect(getFormIntervals.resultFunc(null)).toEqual(null);
    });

    test('должен возвращать null, если нет переговорок', () => {
      const start = Number(new Date(2020, 5, 10, 15, 30));
      const end = Number(new Date(2020, 5, 10, 16, 30));
      const formValues = {start, end};

      expect(getFormIntervals.resultFunc(formValues)).toEqual(null);
    });

    test('должен возвращать null, если 0 переговорок', () => {
      const start = Number(new Date(2020, 5, 10, 15, 30));
      const end = Number(new Date(2020, 5, 10, 16, 30));
      const formValues = {start, end, resources: []};

      expect(getFormIntervals.resultFunc(formValues)).toEqual(null);
    });
  });

  describe('getIntervals', () => {
    test('должен возвращать интервалы для заданной даты', () => {
      const resources = {
        intervals: {
          777: {}
        }
      };

      expect(getIntervals.resultFunc(resources, 777)).toEqual({});
    });
    test('должен возвращать пустой объект, если нет интервалов для заданной даты', () => {
      const resources = {
        intervals: {
          777: {}
        }
      };

      expect(getIntervals.resultFunc(resources, 778)).toEqual({});
    });
    test('должен добавлять интервалы, полученные из формы интервалы для заданной даты', () => {
      const resource1Email = 'pewpew@ya.ru';
      const resource2Email = 'pewpewpew@ya.ru';
      const date = Number(new Date(2020, 5, 10));
      const interval1 = Symbol();
      const interval2 = Symbol();
      const resources = {
        intervals: {
          [date]: {
            [resource1Email]: [interval1],
            [resource2Email]: []
          }
        }
      };
      const formIntervals = {
        [resource1Email]: [interval2]
      };
      const expectedIntervalsMap = {
        [resource1Email]: [interval1, interval2],
        [resource2Email]: []
      };

      expect(getIntervals.resultFunc(resources, date, formIntervals)).toEqual(expectedIntervalsMap);
    });
  });

  describe('isInvite', () => {
    test('должен возвращать true для страницы invite', () => {
      const state = {
        router: {
          location: {
            pathname: '/invite'
          }
        }
      };

      expect(isInvite(state)).toEqual(true);
    });
    test('должен возвращать true для страницы внутри invite', () => {
      const state = {
        router: {
          location: {
            pathname: '/invite/create'
          }
        }
      };

      expect(isInvite(state)).toEqual(true);
    });
    test('должен возвращать false для прочих invite', () => {
      const state = {
        router: {
          location: {
            pathname: '/'
          }
        }
      };

      expect(isInvite(state)).toEqual(false);
    });

    describe('getOfficeIdFromQuery', () => {
      test('должен возвращать приведённый к числу параметр office', () => {
        const routerLocation = {
          search: '?office=345'
        };

        expect(getOfficeIdFromQuery.resultFunc(routerLocation)).toEqual(345);
      });

      test('должен возвращать null, если нет параметра office', () => {
        const routerLocation = {
          search: '?sdfsf=345'
        };

        expect(getOfficeIdFromQuery.resultFunc(routerLocation)).toEqual(null);
      });

      test('должен возвращать null, если нет параметр office - не число', () => {
        const routerLocation = {
          search: '?office=bc_benua'
        };

        expect(getOfficeIdFromQuery.resultFunc(routerLocation)).toEqual(null);
      });
    });
  });
});
