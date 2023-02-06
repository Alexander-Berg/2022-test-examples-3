import Decision from '../../../../constants/Decision';
import EventRecord from '../../../events/EventRecord';
import getInitialValuesForEventFormPreview from '../getInitialValuesForEventFormPreview';

const event = new EventRecord({
  id: 'common',
  externalId: 'ex_common',
  layerId: 'layer1',
  startTs: '2016-10-11T10:00:00',
  endTs: '2016-10-11T10:30:00',
  instanceStartTs: '2016-10-11T10:00:00',
  attendees: {
    'myEmail@ya.ru': {
      email: 'myEmail@ya.ru',
      decision: Decision.UNDECIDED
    },
    'otherEmail@ya.ru': {
      email: 'otherEmail@ya.ru',
      decision: Decision.UNDECIDED
    }
  },
  optionalAttendees: {
    'secondEmail@ya.ru': {
      email: 'secondEmail@ya.ru',
      decision: Decision.UNDECIDED
    },
    'thirdEmail@ya.ru': {
      email: 'thirdEmail@ya.ru',
      decision: Decision.UNDECIDED
    }
  },
  repetition: {
    type: 'MONTHLY'
  },
  resources: [],
  totalAttendees: 2,
  totalOptionalAttendees: 2,
  data: null
});

describe('getInitialValuesForEventFormPreview', () => {
  test('должен возвращать null, если не передан event', () => {
    expect(getInitialValuesForEventFormPreview(undefined)).toEqual(null);
  });

  test('должен возвращать initialValues', () => {
    const module = require('../processInputRepetition');
    const processInputRepetition = jest.spyOn(module, 'default');

    processInputRepetition.mockReturnValue({
      type: 'MONTHLY',
      dueDate: null,
      daily: {each: 1, every: 'everyNDay'},
      weekly: {each: 1, weeklyDays: 'tue'},
      monthly: {
        eachDay: 1,
        eachWeekDay: 1,
        monthlyLastweek: false,
        every: 'everyNthDay'
      },
      yearly: {each: 1}
    });

    const initialValues = getInitialValuesForEventFormPreview(event);

    expect(initialValues).toEqual({
      id: event.id,
      attendees: [
        {email: 'myEmail@ya.ru', decision: 'undecided'},
        {email: 'otherEmail@ya.ru', decision: 'undecided'}
      ],
      optionalAttendees: [
        {email: 'secondEmail@ya.ru', decision: 'undecided'},
        {email: 'thirdEmail@ya.ru', decision: 'undecided'}
      ],
      totalAttendees: 2,
      totalOptionalAttendees: 2,
      description: event.description,
      descriptionHtml: event.descriptionHtml,
      end: event.presentationEnd,
      isAllDay: event.isAllDay,
      layerId: event.layerId,
      location: event.location,
      locationHtml: event.locationHtml,
      name: event.name,
      notifications: event.notifications,
      organizer: event.organizer,
      repeat: Boolean(event.repetition),
      repetition: {
        type: 'MONTHLY',
        dueDate: null,
        daily: {each: 1, every: 'everyNDay'},
        weekly: {each: 1, weeklyDays: 'tue'},
        monthly: {
          eachDay: 1,
          eachWeekDay: 1,
          monthlyLastweek: false,
          every: 'everyNthDay'
        },
        yearly: {each: 1}
      },
      resources: [],
      start: event.start,
      shouldCheckAvailability: false,
      othersCanView: event.othersCanView,
      availability: event.availability,
      eventData: null
    });
  });
});
