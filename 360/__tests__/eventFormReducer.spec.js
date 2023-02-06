import Availability from 'constants/Availability';
import Decision from 'constants/Decision';
import {ActionTypes as EventsActionTypes} from 'features/events/eventsConstants';
import {Gaps} from 'constants/Gaps';
import WorkMods from 'constants/WorkMods';

import * as actions from '../eventFormActions';
import eventFormReducer from '../eventFormReducer';
import EventFormId from '../EventFormId';
import {RESOURCE_SIZE_ERRORS} from '../eventFormConstants';

const form = EventFormId.fromParams(EventFormId.VIEWS.PAGE, EventFormId.MODES.CREATE).toString();
const otherForm = EventFormId.fromParams(
  EventFormId.VIEWS.POPUP,
  EventFormId.MODES.CREATE
).toString();

describe('eventFormReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(eventFormReducer(undefined, {})).toEqual({});
  });
  test('должен не падать и не изменять стейт если формы уже/еще не существует', () => {
    const action = {
      type: actions.makeCheckMembersAvailabilitySuccess.type,
      payload: {
        membersAvailability: [
          {
            email: 'test@ya.ru',
            availability: Availability.BUSY
          }
        ]
      },
      meta: {
        form: otherForm
      }
    };
    const state = {
      [form]: {
        initial: {},
        values: {
          name: 'new event',
          attendees: [
            {
              email: 'test@ya.ru',
              availability: Availability.MAYBE
            }
          ]
        }
      }
    };

    expect(eventFormReducer(state, action)).toEqual(state);
  });

  test('должен влиять только на форму из meta.form', () => {
    const action = {
      type: actions.makeCheckMembersAvailabilitySuccess.type,
      payload: {
        membersAvailability: [
          {
            email: 'test@ya.ru',
            availability: Availability.BUSY
          }
        ]
      },
      meta: {form}
    };
    const state = {
      [otherForm]: {
        initial: {},
        values: {
          name: 'new event',
          attendees: [
            {
              email: 'test@ya.ru',
              availability: Availability.MAYBE
            }
          ],
          optionalAttendees: []
        }
      },
      [form]: {
        initial: {},
        values: {
          name: 'new event',
          attendees: [
            {
              email: 'test@ya.ru',
              availability: Availability.MAYBE
            }
          ],
          optionalAttendees: []
        }
      }
    };
    const expectedState = {
      [otherForm]: {
        initial: {},
        values: {
          name: 'new event',
          attendees: [
            {
              email: 'test@ya.ru',
              availability: Availability.MAYBE
            }
          ],
          optionalAttendees: []
        }
      },
      [form]: {
        initial: {},
        values: {
          name: 'new event',
          attendees: [
            {
              email: 'test@ya.ru',
              availability: Availability.BUSY
            }
          ],
          optionalAttendees: []
        }
      }
    };

    expect(eventFormReducer(state, action)).toEqual(expectedState);
  });

  describe('makeSetResourcesAutofit', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: actions.makeSetResourcesAutofit.type,
        payload: {},
        meta: {form}
      };
      const state = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(state);
    });

    test('должен устанакливать автоподбор выключенным, если статус не ACCEPTED', () => {
      const action = {
        type: actions.makeSetResourcesAutofit.type,
        payload: {officeIds: [1, 2], status: 'FOO'},
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {
            resources: []
          },
          values: {
            resources: []
          }
        }
      };

      const expectedState = {
        [form]: {
          initial: {
            resources: [],
            isAutofitOn: false
          },
          values: {
            resources: [],
            isAutofitOn: false,
            isAutofitEnabled: false
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен устанакливать автоподбор включенным, если статус ACCEPTED, и добавлять в форму офисы из автоподбора', () => {
      const action = {
        type: actions.makeSetResourcesAutofit.type,
        payload: {officeIds: [1, 2], status: 'ACCEPTED'},
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {
            resources: []
          },
          values: {
            resources: []
          }
        }
      };

      const expectedState = {
        [form]: {
          initial: {
            resources: [
              {
                officeId: 1,
                resource: null
              },
              {
                officeId: 2,
                resource: null
              }
            ],
            isAutofitOn: true
          },
          values: {
            isAutofitOn: true,
            isAutofitEnabled: true,
            resources: [
              {
                officeId: 1,
                resource: null
              },
              {
                officeId: 2,
                resource: null
              }
            ]
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('setGaps', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: actions.makeSetGaps.type,
        payload: {
          gaps: {},
          isMixedAsRemote: false,
          start: 10
        },
        meta: {form}
      };
      const state = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(state);
    });

    test('должен корректно отрабатывать, если данные гэпов пустые', () => {
      const action = {
        type: actions.makeSetGaps.type,
        payload: {
          gaps: {},
          isMixedAsRemote: false,
          start: 10
        },
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            attendees: [
              {
                login: 'tavria'
              }
            ],
            organizer: {
              login: 'tet4enko'
            }
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            attendees: [
              {
                login: 'tavria',
                gap: null
              }
            ],
            organizer: {
              login: 'tet4enko',
              gap: null
            }
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен отрабатывать и для участников, и для организатора', () => {
      const action = {
        type: actions.makeSetGaps.type,
        payload: {
          gaps: {
            tavria: {
              workMode: WorkMods.REMOTE,
              gaps: [
                {
                  type: Gaps.TRIP,
                  isAllDay: true
                }
              ]
            },
            tet4enko: {
              workMode: WorkMods.REMOTE,
              gaps: [
                {
                  type: Gaps.CONFERENCE,
                  isAllDay: true
                }
              ]
            }
          },
          isMixedAsRemote: false,
          start: 10
        },
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            attendees: [
              {
                login: 'tavria'
              }
            ],
            organizer: {
              login: 'tet4enko'
            }
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            attendees: [
              {
                login: 'tavria',
                gap: {
                  caption: '1 января в командировке',
                  color: '#aad46e',
                  type: 'trip',
                  isAllDay: true
                }
              }
            ],
            organizer: {
              login: 'tet4enko',
              gap: {
                caption: '1 января на конференции',
                color: '#aad46e',
                type: 'conference',
                isAllDay: true
              }
            }
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('checkMembersAvailabilitySuccess', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: actions.makeCheckMembersAvailabilitySuccess.type,
        payload: {
          membersAvailability: [
            {
              email: 'test@ya.ru',
              availability: Availability.BUSY
            }
          ]
        },
        meta: {form}
      };
      const state = {[form]: null};
      const expectedState = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновлять занятость участников', () => {
      const action = {
        type: actions.makeCheckMembersAvailabilitySuccess.type,
        payload: {
          membersAvailability: [
            {
              email: 'test@ya.ru',
              availability: Availability.BUSY
            }
          ]
        },
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            attendees: [
              {
                email: 'test@ya.ru',
                availability: Availability.MAYBE
              }
            ],
            optionalAttendees: []
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            attendees: [
              {
                email: 'test@ya.ru',
                availability: Availability.BUSY
              }
            ],
            optionalAttendees: []
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('checkAvailabilitiesSuccess', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: actions.makeCheckAvailabilitiesSuccess.type,
        payload: {
          availabilities: [
            {
              email: 'test@ya.ru',
              availability: Availability.BUSY
            }
          ]
        },
        meta: {form}
      };
      const state = {[form]: null};
      const expectedState = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновлять занятость участников', () => {
      const action = {
        type: actions.makeCheckAvailabilitiesSuccess.type,
        payload: {
          availabilities: [
            {
              email: 'test@ya.ru',
              availability: Availability.BUSY
            }
          ]
        },
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            attendees: [
              {
                email: 'test2@ya.ru',
                availability: Availability.MAYBE
              },
              {
                email: 'test@ya.ru',
                availability: Availability.MAYBE
              }
            ],
            optionalAttendees: []
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            attendees: [
              {
                email: 'test2@ya.ru',
                availability: Availability.MAYBE
              },
              {
                email: 'test@ya.ru',
                availability: Availability.BUSY
              }
            ],
            optionalAttendees: []
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновлять занятость организатора', () => {
      const action = {
        type: actions.makeCheckAvailabilitiesSuccess.type,
        payload: {
          availabilities: [
            {
              email: 'test2@ya.ru',
              availability: Availability.BUSY
            }
          ]
        },
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            organizer: {
              email: 'test2@ya.ru',
              availability: Availability.MAYBE
            },
            attendees: [
              {
                email: 'test@ya.ru',
                availability: Availability.MAYBE
              }
            ],
            optionalAttendees: []
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            organizer: {
              email: 'test2@ya.ru',
              availability: Availability.BUSY
            },
            attendees: [
              {
                email: 'test@ya.ru',
                availability: Availability.MAYBE
              }
            ],
            optionalAttendees: []
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновлять занятость переговорок', () => {
      const action = {
        type: actions.makeCheckAvailabilitiesSuccess.type,
        payload: {
          availabilities: [
            {
              email: 'confroom@ya.ru',
              availability: Availability.AVAILABLE,
              dueDate: '23',
              availableRepetitions: 10
            }
          ]
        },
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            organizer: {
              email: 'test2@ya.ru',
              availability: Availability.MAYBE
            },
            attendees: [
              {
                email: 'test@ya.ru',
                availability: Availability.MAYBE
              }
            ],
            optionalAttendees: [],
            resources: [
              {
                resource: {
                  email: 'confroom@ya.ru'
                }
              }
            ]
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            organizer: {
              email: 'test2@ya.ru',
              availability: Availability.MAYBE
            },
            attendees: [
              {
                email: 'test@ya.ru',
                availability: Availability.MAYBE
              }
            ],
            optionalAttendees: [],
            resources: [
              {
                resource: {
                  email: 'confroom@ya.ru',
                  availability: Availability.AVAILABLE,
                  dueDate: '23',
                  availableRepetitions: 10
                }
              }
            ]
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('не должен добавлять лишних полей в орга и участников', () => {
      const action = {
        type: actions.makeCheckAvailabilitiesSuccess.type,
        payload: {
          availabilities: [
            {
              email: 'test@ya.ru',
              availability: Availability.AVAILABLE,
              dueDate: '23',
              availableRepetitions: 10
            },
            {
              email: 'test2@ya.ru',
              availability: Availability.AVAILABLE,
              dueDate: '23',
              availableRepetitions: 10
            }
          ]
        },
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            organizer: {
              email: 'test2@ya.ru',
              availability: Availability.MAYBE
            },
            attendees: [
              {
                email: 'test@ya.ru',
                availability: Availability.MAYBE
              }
            ],
            optionalAttendees: [],
            resources: [
              {
                resource: {
                  email: 'confroom@ya.ru'
                }
              }
            ]
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            organizer: {
              email: 'test2@ya.ru',
              availability: Availability.AVAILABLE
            },
            attendees: [
              {
                email: 'test@ya.ru',
                availability: Availability.AVAILABLE
              }
            ],
            optionalAttendees: [],
            resources: [
              {
                resource: {
                  email: 'confroom@ya.ru'
                }
              }
            ]
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('checkOrganizerAvailabilitySuccess', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: actions.makeCheckOrganizerAvailabilitySuccess.type,
        payload: {
          email: 'test@ya.ru',
          availability: Availability.BUSY
        }
      };
      const state = {[form]: null};
      const expectedState = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновлять занятость организатора', () => {
      const action = {
        type: actions.makeCheckOrganizerAvailabilitySuccess.type,
        payload: {
          email: 'test@ya.ru',
          availability: Availability.BUSY
        },
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            organizer: {
              email: 'test@ya.ru',
              availability: Availability.MAYBE
            }
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            name: 'new event',
            organizer: {
              email: 'test@ya.ru',
              availability: Availability.BUSY
            }
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('checkResourcesAvailabilitySuccess', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: actions.makeCheckResourcesAvailabilitySuccess.type,
        payload: {
          resourcesAvailability: [
            {
              email: 'zimniy@yandex-team.ru',
              availability: Availability.BUSY
            }
          ]
        },
        meta: {form}
      };
      const state = {[form]: null};
      const expectedState = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновлять занятость переговорок', () => {
      const action = {
        type: actions.makeCheckResourcesAvailabilitySuccess.type,
        payload: {
          resourcesAvailability: [
            {
              email: 'zimniy@yandex-team.ru',
              availability: Availability.AVAILABLE,
              availableRepetitions: 5,
              dueDate: '2017-12-30'
            }
          ]
        },
        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            resources: [
              {
                office: 4,
                resource: null
              },
              {
                officeId: 2,
                resource: {
                  email: 'zimniy@yandex-team.ru',
                  availability: Availability.BUSY
                }
              },
              {
                officeId: 1,
                resource: {
                  email: 'pomidor@yandex-team.ru',
                  availability: Availability.AVAILABLE
                }
              }
            ]
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            resources: [
              {
                office: 4,
                resource: null
              },
              {
                officeId: 2,
                resource: {
                  email: 'zimniy@yandex-team.ru',
                  availability: Availability.AVAILABLE,
                  availableRepetitions: 5,
                  dueDate: '2017-12-30'
                }
              },
              {
                officeId: 1,
                resource: {
                  email: 'pomidor@yandex-team.ru',
                  availability: Availability.AVAILABLE
                }
              }
            ]
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_DECISION_SUCCESS', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: EventsActionTypes.UPDATE_DECISION_SUCCESS,
        myEmail: 'test@ya.ru',
        decision: Decision.YES
      };
      const state = {[form]: null};
      const expectedState = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновлять решение пользователя', () => {
      const action = {
        type: EventsActionTypes.UPDATE_DECISION_SUCCESS,
        myEmail: 'test@ya.ru',
        decision: Decision.YES,
        meta: {
          form
        }
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            attendees: [
              {
                email: 'test@ya.ru',
                decision: Decision.MAYBE
              },
              {
                email: 'test_2@ya.ru',
                decision: Decision.NO
              }
            ]
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            attendees: [
              {
                email: 'test@ya.ru',
                decision: Decision.YES
              },
              {
                email: 'test_2@ya.ru',
                decision: Decision.NO
              }
            ]
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });
  describe('SET_LOCATION_URI', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: actions.setLocationUri.type,
        payload: 'ymapdbm1://some'
      };
      const state = {[form]: null};
      const expectedState = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновлять location uri', () => {
      const action = {
        type: actions.setLocationUri.type,
        payload: {uri: 'ymapdbm1://some'},

        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {
            eventData: {
              location: {
                uri: null
              }
            }
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            eventData: {
              location: {
                uri: 'ymapdbm1://some'
              }
            }
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('makeCheckResourcesSize', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: actions.makeCheckResourcesSize.type,
        payload: {
          resourceValidationConfig: {
            '8': {
              criticalDiff: 3
            }
          },
          moscowOffices: [1, 8, 9]
        },
        meta: {form}
      };

      const state = {[form]: null};
      const expectedState = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен отрабатывать только на БЦ из конфига', () => {
      const action = {
        type: actions.makeCheckResourcesSize.type,
        payload: {
          resourceValidationConfig: {
            '8': {
              criticalDiff: 3
            }
          },
          moscowOffices: [1, 8, 9]
        },
        meta: {form}
      };

      const state = {
        [form]: {
          initial: {},
          values: {
            organizer: {
              login: 'tet4enko',
              officeId: 1
            },
            attendees: [
              {
                login: 'tavria',
                officeId: 8
              }
            ],
            resources: [
              {
                officeId: 10,
                resource: {
                  name: 'Red Hot Chili Peppers',
                  seats: 9999
                }
              },
              {
                officeId: 8,
                resource: {
                  name: 'Чердак',
                  seats: 9999
                }
              }
            ]
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            organizer: {
              login: 'tet4enko',
              officeId: 1
            },
            attendees: [
              {
                login: 'tavria',
                officeId: 8
              }
            ],
            resources: [
              {
                officeId: 10,
                resource: {
                  name: 'Red Hot Chili Peppers',
                  seats: 9999,
                  error: null
                }
              },
              {
                officeId: 8,
                resource: {
                  name: 'Чердак',
                  seats: 9999,
                  error: RESOURCE_SIZE_ERRORS.TOO_LARGE
                }
              }
            ]
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен учитывать организатора', () => {
      const action = {
        type: actions.makeCheckResourcesSize.type,
        payload: {
          resourceValidationConfig: {
            '8': {
              criticalDiff: 3
            }
          },
          moscowOffices: [1, 8, 9]
        },
        meta: {form}
      };

      const state = {
        [form]: {
          initial: {},
          values: {
            organizer: {
              login: 'tet4enko',
              officeId: 8
            },
            attendees: [
              {
                login: 'tavria',
                officeId: 8
              },
              {
                login: 'chapson',
                officeId: 8
              }
            ],
            resources: [
              {
                officeId: 8,
                resource: {
                  name: 'Чердак',
                  seats: 2
                }
              }
            ]
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            organizer: {
              login: 'tet4enko',
              officeId: 8
            },
            attendees: [
              {
                login: 'tavria',
                officeId: 8
              },
              {
                login: 'chapson',
                officeId: 8
              }
            ],
            resources: [
              {
                officeId: 8,
                resource: {
                  name: 'Чердак',
                  seats: 2,
                  error: RESOURCE_SIZE_ERRORS.TOO_SMALL
                }
              }
            ]
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен учитывать параметр criticalDiff из конфига', () => {
      const action = {
        type: actions.makeCheckResourcesSize.type,
        payload: {
          resourceValidationConfig: {
            '8': {
              criticalDiff: 4
            }
          },
          moscowOffices: [1, 8, 9]
        },
        meta: {form}
      };

      const state = {
        [form]: {
          initial: {},
          values: {
            organizer: {
              login: 'tet4enko',
              officeId: 8
            },
            attendees: [
              {
                login: 'tavria',
                officeId: 8
              },
              {
                login: 'chapson',
                officeId: 8
              }
            ],
            resources: [
              {
                officeId: 8,
                resource: {
                  name: 'Чердак',
                  seats: 5
                }
              }
            ]
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            organizer: {
              login: 'tet4enko',
              officeId: 8
            },
            attendees: [
              {
                login: 'tavria',
                officeId: 8
              },
              {
                login: 'chapson',
                officeId: 8
              }
            ],
            resources: [
              {
                officeId: 8,
                resource: {
                  name: 'Чердак',
                  seats: 5,
                  error: null
                }
              }
            ]
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен выводить отдельную ошибку для переговорки без участников', () => {
      const action = {
        type: actions.makeCheckResourcesSize.type,
        payload: {
          resourceValidationConfig: {
            '8': {
              criticalDiff: 4
            }
          },
          moscowOffices: [1, 8, 9]
        },
        meta: {form}
      };

      const state = {
        [form]: {
          initial: {},
          values: {
            organizer: {
              login: 'tet4enko',
              officeId: 2
            },
            attendees: [
              {
                login: 'tavria',
                officeId: 2
              },
              {
                login: 'chapson',
                officeId: 2
              }
            ],
            resources: [
              {
                officeId: 8,
                resource: {
                  name: 'Чердак',
                  seats: 5
                }
              }
            ]
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            organizer: {
              login: 'tet4enko',
              officeId: 2
            },
            attendees: [
              {
                login: 'tavria',
                officeId: 2
              },
              {
                login: 'chapson',
                officeId: 2
              }
            ],
            resources: [
              {
                officeId: 8,
                resource: {
                  name: 'Чердак',
                  seats: 5,
                  error: RESOURCE_SIZE_ERRORS.IS_EMPTY
                }
              }
            ]
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен учитывать фактор московского офиса при выводе ошибки для переговорки без участников', () => {
      const action = {
        type: actions.makeCheckResourcesSize.type,
        payload: {
          resourceValidationConfig: {
            '1': {
              criticalDiff: 4
            }
          },
          moscowOffices: [1, 8, 9]
        },
        meta: {form}
      };

      const state = {
        [form]: {
          initial: {},
          values: {
            organizer: {
              login: 'tet4enko',
              officeId: 8
            },
            attendees: [
              {
                login: 'tavria',
                officeId: 8
              }
            ],
            resources: [
              {
                officeId: 1,
                resource: {
                  name: 'Red Hot Chili Peppers',
                  seats: 7
                }
              }
            ]
          }
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            organizer: {
              login: 'tet4enko',
              officeId: 8
            },
            attendees: [
              {
                login: 'tavria',
                officeId: 8
              }
            ],
            resources: [
              {
                officeId: 1,
                resource: {
                  name: 'Red Hot Chili Peppers',
                  seats: 7,
                  error: RESOURCE_SIZE_ERRORS.TOO_LARGE
                }
              }
            ]
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('makeSetZoomCreatorPlanType', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: actions.makeSetZoomCreatorPlanType.type,
        payload: {type: 1}
      };
      const state = {[form]: null};
      const expectedState = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновлять zoomCreatorPlanType', () => {
      const action = {
        type: actions.makeSetZoomCreatorPlanType.type,
        payload: {type: 1},

        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {}
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            zoomCreatorPlanType: 1
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('makeResetAddZoomLink', () => {
    test('не должен обрабатывать, если стейт пустой', () => {
      const action = {
        type: actions.makeResetAddZoomLink.type
      };
      const state = {[form]: null};
      const expectedState = {[form]: null};

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
    test('должен сбрасывать в false addZoomLink', () => {
      const action = {
        type: actions.makeResetAddZoomLink.type,

        meta: {form}
      };
      const state = {
        [form]: {
          initial: {},
          values: {addZoomLink: true}
        }
      };
      const expectedState = {
        [form]: {
          initial: {},
          values: {
            addZoomLink: false
          }
        }
      };

      expect(eventFormReducer(state, action)).toEqual(expectedState);
    });
  });
});
