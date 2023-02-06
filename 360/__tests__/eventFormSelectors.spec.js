import moment from 'moment';
import {Map} from 'immutable';

import {isMyEmail} from 'utils/emails';
import Decision from 'constants/Decision';

import {
  makeGetInviteeEmails,
  makeGetConferenceDuration,
  makeGetShouldUseRepetition,
  makeEventFormValuesSelector,
  isPristineCreationSelector,
  makeHasTelemostLink,
  makeZoomCreatorPlanType,
  makeGetStartDay,
  getEventForm
} from '../eventFormSelectors';
import EventFormId from '../EventFormId';

jest.mock('utils/emails');
isMyEmail.mockImplementation(email => email === 'own@yandex-team.ru');
const form = EventFormId.fromParams(EventFormId.VIEWS.POPUP, EventFormId.MODES.CREATE).toString();

describe('eventFormSelectors', () => {
  describe('getInviteeEmails', () => {
    test('должен возвращать email-адреса начальных участников и организатора, не отказавшихся от встречи', () => {
      const state = {
        form: {
          [form]: {
            initial: {
              attendees: [
                {
                  email: 'some-user@yandex-team.ru',
                  decision: Decision.MAYBE
                },
                {
                  email: 'some-other-user@yandex-team.ru',
                  decision: Decision.YES
                }
              ],
              organizer: {
                email: 'organizer@yandex-team.ru',
                decision: Decision.UNDECIDED
              }
            }
          }
        }
      };

      expect(makeGetInviteeEmails(form)(state)).toEqual([
        'organizer@yandex-team.ru',
        'some-user@yandex-team.ru',
        'some-other-user@yandex-team.ru'
      ]);
    });

    test('не должен ломаться при событии без организатора и участников', () => {
      const state = {
        form: {
          [form]: {
            initial: {
              attendees: [],
              organizer: null
            }
          }
        }
      };

      expect(makeGetInviteeEmails(form)(state)).toEqual([]);
    });

    test('не должен возвращать email текущего пользователя', () => {
      const state = {
        form: {
          [form]: {
            initial: {
              attendees: [
                {
                  email: 'own@yandex-team.ru',
                  decision: Decision.YES
                }
              ],
              organizer: {
                email: 'some-user@yandex-team.ru',
                decision: Decision.YES
              }
            }
          }
        }
      };

      expect(makeGetInviteeEmails(form)(state)).toEqual(['some-user@yandex-team.ru']);
    });

    test('не должен возвращать адреса отказавшихся прийти', () => {
      const state = {
        form: {
          [form]: {
            initial: {
              attendees: [
                {
                  email: 'some-user@yandex-team.ru',
                  decision: Decision.NO
                }
              ],
              organizer: {
                email: 'some-other-user@yandex-team.ru',
                decision: Decision.NO
              }
            }
          }
        }
      };

      expect(makeGetInviteeEmails(form)(state)).toEqual([]);
    });
  });

  describe('getConferenceDuration', () => {
    test('должен возвращать 0, если событие еще не началось', () => {
      const state = {
        form: {
          [form]: {
            values: {
              start: Number(moment('2018-01-01T10:00')),
              end: Number(moment('2018-01-01T10:30'))
            }
          }
        },
        datetime: new Map({
          time: Number(moment('2018-01-01T09:00'))
        })
      };

      expect(makeGetConferenceDuration(form)(state)).toEqual(0);
    });

    test('должен возвращать 0, если событие уже закончилось', () => {
      const state = {
        form: {
          [form]: {
            values: {
              start: Number(moment('2018-01-01T10:00')),
              end: Number(moment('2018-01-01T10:30'))
            }
          }
        },
        datetime: new Map({
          time: Number(moment('2018-01-01T11:00'))
        })
      };

      expect(makeGetConferenceDuration(form)(state)).toEqual(0);
    });

    test('должен возвращать длительность конференции с текущего момента и до конца события в минутах', () => {
      const state = {
        form: {
          [form]: {
            values: {
              start: Number(moment('2018-01-01T10:00')),
              end: Number(moment('2018-01-01T10:30'))
            }
          }
        },
        datetime: new Map({
          time: Number(moment('2018-01-01T10:05'))
        })
      };

      expect(makeGetConferenceDuration(form)(state)).toEqual(25);
    });
  });
  describe('getShouldUseRepetition', () => {
    describe('создание события', () => {
      test('должен возвращать true при включенном повторении', () => {
        const state = {
          form: {
            [form]: {
              values: {
                repeat: true
              },
              initial: {}
            }
          },
          router: {
            location: {
              search: 'applyToFuture=0'
            }
          }
        };

        expect(makeGetShouldUseRepetition(form)(state)).toEqual(true);
      });
      test('должен возвращать false при выключенном повторении', () => {
        const state = {
          form: {
            [form]: {
              values: {
                repeat: false
              },
              initial: {}
            }
          },
          router: {
            location: {
              search: 'applyToFuture=1'
            }
          }
        };

        expect(makeGetShouldUseRepetition(form)(state)).toEqual(false);
      });
    });
    describe('редактирование события', () => {
      test('должен возвращать true для повторяющегося события при applyToFuture', () => {
        const state = {
          form: {
            [form]: {
              values: {
                id: 777,
                repeat: true
              },
              initial: {
                repeat: true
              }
            }
          },
          router: {
            location: {
              search: 'applyToFuture=1'
            }
          }
        };

        expect(makeGetShouldUseRepetition(form)(state)).toEqual(true);
      });
      test('должен возвращать false для повторяющегося события без applyToFuture', () => {
        const state = {
          form: {
            [form]: {
              values: {
                id: 777,
                repeat: true
              },
              initial: {
                repeat: true
              }
            }
          },
          router: {
            location: {
              search: 'applyToFuture=0'
            }
          }
        };

        expect(makeGetShouldUseRepetition(form)(state)).toEqual(false);
      });
      test('должен возвращать true если в процессе редактирования появилось повторение', () => {
        const state = {
          form: {
            [form]: {
              values: {
                id: 777,
                repeat: true
              },
              initial: {
                repeat: false
              }
            }
          },
          router: {
            location: {
              search: 'applyToFuture=0'
            }
          }
        };

        expect(makeGetShouldUseRepetition(form)(state)).toEqual(true);
      });
      test('должен возвращать false если в процессе редактирования выключили повторение', () => {
        const state = {
          form: {
            [form]: {
              values: {
                id: 777,
                repeat: false
              },
              initial: {
                repeat: true
              }
            }
          },
          router: {
            location: {
              search: 'applyToFuture=1'
            }
          }
        };

        expect(makeGetShouldUseRepetition(form)(state)).toEqual(false);
      });
    });
  });
  describe('makeEventFormValuesSelector', () => {
    const {getEventFormValue, getReservableTable, getEventFormValues} = makeEventFormValuesSelector(
      'form'
    );
    const formValues = {
      attendees: ['pistch@yandex-team.ru'],
      start: 1590568630973,
      isAllDay: false
    };
    describe('getReservableTable', () => {
      test('должен вернуть null, если нет значений', () => {
        expect(getReservableTable.resultFunc(null)).toBe(null);
      });
      test('должен вернуть стол, если он входит в [2]', () => {
        expect(
          getReservableTable.resultFunc({resources: [{officeId: 1047, resource: {}}]})
        ).toEqual({
          officeId: 1047,
          resource: {}
        });
      });
    });
    test('getEventFormValue', () => {
      expect(getEventFormValue.resultFunc({prop: 1}, 'prop')).toBe(1);
    });
    describe('getEventFormValues', () => {
      test('должен вернуть переданный formValues, если не столов', () => {
        const actual = getEventFormValues.resultFunc(null, formValues);
        expect(actual).toBe(formValues);
      });
      test('должен вернуть переданный formValues, если repeat', () => {
        const actual = getEventFormValues.resultFunc(null, {formValues, repeat: true});
        expect(actual).toEqual({formValues, repeat: true});
      });
      test('должен вернуть модифицированный formValues, если передан стол и !repeat', () => {
        const actual = getEventFormValues.resultFunc(
          {
            resource: {
              email: 'someresource@yandex-team.ru'
            }
          },
          formValues
        );
        expect(actual).toEqual({
          ...formValues,
          start: 1590526800000,
          end: 1590613140000,
          isAllDay: true,
          attendees: [],
          availability: 'available'
        });
      });
    });
  });

  describe('isPristineCreationSelector', () => {
    test('должен возвращать false, если перешли к полной форме из попапа', () => {
      const routerLocation = {
        search: '?moreParams=1'
      };

      expect(isPristineCreationSelector.resultFunc(routerLocation)).toBe(false);
    });
  });

  describe('makeHasTelemostLink', () => {
    test('должен возвращать false, если нет eventData', () => {
      const formName = 'some_test_form';
      const state = {
        form: {
          [formName]: {
            values: {}
          }
        }
      };
      const selector = makeHasTelemostLink(formName);

      expect(selector(state)).toBeFalsy();
    });

    test('должен возвращать false, если нет флага в eventData', () => {
      const formName = 'some_test_form';
      const state = {
        form: {
          [formName]: {
            values: {
              eventData: {}
            }
          }
        }
      };
      const selector = makeHasTelemostLink(formName);

      expect(selector(state)).toBeFalsy();
    });

    test('должен возвращать false, если таково значение флага в eventData', () => {
      const formName = 'some_test_form';
      const state = {
        form: {
          [formName]: {
            values: {
              eventData: {
                hasTelemostLink: false
              }
            }
          }
        }
      };
      const selector = makeHasTelemostLink(formName);

      expect(selector(state)).toBeFalsy();
    });

    test('должен возвращать true, если таково значение флага в eventData', () => {
      const formName = 'some_test_form';
      const state = {
        form: {
          [formName]: {
            values: {
              eventData: {
                hasTelemostLink: true
              }
            }
          }
        }
      };
      const selector = makeHasTelemostLink(formName);

      expect(selector(state)).toBeTruthy();
    });
  });

  describe('makeZoomCreatorPlanType', () => {
    test('должен возвращать null, если нет zoomCreatorPlanType', () => {
      const formName = 'some_test_form';
      const state = {
        form: {
          [formName]: {
            values: {}
          }
        }
      };
      const selector = makeZoomCreatorPlanType(formName);

      expect(selector(state)).toBe(null);
    });
  });

  describe('makeGetStartDay', () => {
    test('должен возвращать null, если нет start', () => {
      const formName = 'some_test_form';
      const state = {
        form: {
          [formName]: {
            values: {}
          }
        }
      };
      const selector = makeGetStartDay(formName);

      expect(selector(state)).toBe(null);
    });

    test('должен возвращать начало дня от start', () => {
      const formName = 'some_test_form';
      const start = 1633095606008;
      const state = {
        form: {
          [formName]: {
            values: {
              start
            }
          }
        }
      };
      const selector = makeGetStartDay(formName);

      expect(selector(state)).toBe(Number(moment(start).startOf('day')));
    });
  });

  describe('getEventForm', () => {
    test('должен возвращать null, если форма события не найдена', () => {
      const formName = 'some_test_form';
      const state = {
        form: {
          [formName]: {
            values: {}
          }
        }
      };

      expect(getEventForm(state)).toBe(null);
    });

    test('должен возвращать первую найденную форму события', () => {
      const form1Name = 'some_test_form';
      const form2Name = 'eventform: some_test_form1';
      const form3Name = 'eventform: some_test_form2';
      const state = {
        form: {
          [form1Name]: {
            values: {}
          },
          [form2Name]: {
            values: {}
          },
          [form3Name]: {
            values: {}
          }
        }
      };

      expect(getEventForm(state)).toBe(form2Name);
    });
  });
});
