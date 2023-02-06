import React from 'react';
import {shallow} from 'enzyme';
import moment from 'moment';

import {TimelineWidgetContainer} from '../TimelineWidgetContainer';

const defaultProps = {
  start: moment('2017-09-21T15:01:00').valueOf(),
  end: moment('2017-09-21T15:05:00').valueOf(),
  isAllDay: false,
  organizer: {
    email: 'test1@ya.ru',
    name: 'name1',
    login: 'login1'
  },
  members: [
    {
      email: 'test1@ya.ru',
      name: 'name2',
      login: 'login2'
    },
    {
      email: 'test3@yandex.ru',
      name: 'name3',
      login: 'login3'
    }
  ],
  resources: [
    {
      officeId: 1,
      resource: null
    },
    {
      officeId: 2,
      resource: {
        email: 'room1@yandex-team.ru',
        name: 'name1'
      }
    }
  ],
  eventId: 111,
  getAvailabilityIntervals: () => {}
};

function setup(props) {
  return shallow(<TimelineWidgetContainer {...defaultProps} {...props} />);
}

describe('<TimelineWidgetContainer />', () => {
  describe('_tryResetTimelineDate', () => {
    test('должен вызвать _tryResetTimelineDate при передаче новых свойств компоненту', () => {
      const component = setup();

      jest.spyOn(TimelineWidgetContainer.prototype, '_tryResetTimelineDate').mockReturnValue();
      component.setProps(defaultProps);
      expect(TimelineWidgetContainer.prototype._tryResetTimelineDate).toHaveBeenCalledWith(
        defaultProps
      );
    });

    test('должен изменять дату начала таймлайна, если изменилось время начала события', () => {
      const component = setup();
      const props = {
        ...defaultProps,
        start: moment('2017-09-22T15:01:00').valueOf()
      };

      component.setState({timelineDate: moment('2017-09-10').valueOf()});
      component.instance()._tryResetTimelineDate(props);
      expect(component.state()).toHaveProperty('timelineDate', moment('2017-09-22').valueOf());
    });

    test('должен изменять дату начала таймлайна, если изменилось время конца события', () => {
      const component = setup();
      const props = {
        ...defaultProps,
        end: moment('2017-09-22T15:00:00').valueOf()
      };

      component.setState({timelineDate: moment('2017-09-10').valueOf()});
      component.instance()._tryResetTimelineDate(props);
      expect(component.state()).toHaveProperty('timelineDate', moment('2017-09-21').valueOf());
    });

    test('должен изменять дату начала таймлайна, если изменилось состояние «весь день» у события', () => {
      const component = setup();
      const props = {
        ...defaultProps,
        isAllDay: true
      };

      component.setState({timelineDate: moment('2017-09-10').valueOf()});
      component.instance()._tryResetTimelineDate(props);
      expect(component.state()).toHaveProperty('timelineDate', moment('2017-09-21').valueOf());
    });

    test('должен изменять дату начала таймлайна, если изменились условия повторения события', () => {
      const component = setup();
      const props = {
        ...defaultProps,
        repetition: {
          type: 'weekly'
        }
      };

      component.setState({timelineDate: moment('2017-09-10').valueOf()});
      component.instance()._tryResetTimelineDate(props);
      expect(component.state()).toHaveProperty('timelineDate', moment('2017-09-21').valueOf());
    });

    test('должен сбрасывать интервалы, если изменилась дата начала таймлайна', () => {
      const component = setup();
      const props = {
        ...defaultProps,
        start: moment('2017-09-25T15:01:00').valueOf()
      };

      component.setState({intervals: {interval: 111}});
      component.instance()._tryResetTimelineDate(props);
      expect(component.state()).toHaveProperty('intervals', {});
    });

    test('не должен сбрасывать интервалы, если не изменилась дата начала таймлайна', () => {
      const component = setup();
      const props = {
        ...defaultProps,
        start: moment('2017-09-21T15:01:00').valueOf()
      };

      component.setState({intervals: {interval: 111}});
      component.instance()._tryResetTimelineDate(props);
      expect(component.state()).toHaveProperty('intervals', {interval: 111});
    });
  });

  describe('_tryUpdateIntervals', () => {
    beforeEach(() => {
      jest.spyOn(TimelineWidgetContainer.prototype, '_isEmailsParamChanged');
    });

    test('должен вызвать _tryUpdateIntervals при передаче новых свойств компоненту', () => {
      const component = setup();
      const prevState = component.state();

      jest.spyOn(TimelineWidgetContainer.prototype, '_tryUpdateIntervals');
      component.setProps(defaultProps);
      expect(TimelineWidgetContainer.prototype._tryUpdateIntervals).toHaveBeenCalledWith(
        defaultProps,
        prevState
      );
    });

    test('должен  вызвать загрузку интервалов, если изменилась дата таймлайна', () => {
      const component = setup();
      const prevState = {
        timelineDate: moment('2017-09-22').valueOf()
      };

      component.setState({timelineDate: moment('2017-09-21').valueOf()});
      TimelineWidgetContainer.prototype._isEmailsParamChanged.mockReturnValue(false);
      jest.spyOn(TimelineWidgetContainer.prototype, '_getIntervals');
      component.instance()._tryUpdateIntervals({}, prevState);
      expect(TimelineWidgetContainer.prototype._getIntervals).toHaveBeenCalledTimes(1);
    });

    test('должен вызвать загрузку интервалов, если изменились участники', () => {
      const component = setup();
      component.setState({
        timelineDate: moment('2017-09-21').valueOf()
      });
      const prevState = {
        timelineDate: moment('2017-09-21').valueOf()
      };

      TimelineWidgetContainer.prototype._isEmailsParamChanged.mockReturnValue(true);
      jest.spyOn(TimelineWidgetContainer.prototype, '_getIntervals');
      component.instance()._tryUpdateIntervals({}, prevState);
      expect(TimelineWidgetContainer.prototype._getIntervals).toHaveBeenCalledTimes(1);
    });

    test('не должен вызвать загрузку интервалов, если участники и дата таймлайна не изменились', () => {
      const component = setup();
      component.setState({
        timelineDate: moment('2017-09-21').valueOf()
      });
      const prevState = {
        timelineDate: moment('2017-09-21').valueOf()
      };

      TimelineWidgetContainer.prototype._isEmailsParamChanged.mockReturnValue(false);
      jest.spyOn(TimelineWidgetContainer.prototype, '_getIntervals');
      component.instance()._tryUpdateIntervals({}, prevState);
      expect(TimelineWidgetContainer.prototype._getIntervals).toHaveBeenCalledTimes(0);
    });
  });

  describe('_isEmailsParamChanged', () => {
    test('должен возвращать true, если участники изменились при передаче новых свойств', () => {
      const component = setup();
      const prevProps = {
        ...defaultProps,
        organizer: {
          email: 'test111@ya.ru'
        }
      };
      const nextProps = {
        ...defaultProps,
        organizer: {
          email: 'test222@ya.ru'
        }
      };

      expect(component.instance()._isEmailsParamChanged(prevProps, nextProps)).toBe(true);
    });

    test('должен возвращать false, если участники не изменились при передаче новых свойств', () => {
      const component = setup();

      expect(component.instance()._isEmailsParamChanged(defaultProps, defaultProps)).toBe(false);
    });
  });

  describe('_getEmailsParam', () => {
    test('должен вернуть список уникальных email-ов', () => {
      const component = setup();

      expect(component.instance()._getEmailsParam(defaultProps)).toEqual([
        'test1@ya.ru',
        'test3@yandex.ru',
        'room1@yandex-team.ru'
      ]);
    });
  });

  describe('_getIntervals', () => {
    test('должен вызвать getAvailabilityIntervals и setState c нужными параметрами при запросе интервалов', async () => {
      const func = jest.fn((params, resolve) => {
        resolve([
          {
            email: 'test@yandex.ru',
            intervals: []
          }
        ]);
      });
      const component = setup({getAvailabilityIntervals: func});

      jest.spyOn(TimelineWidgetContainer.prototype, 'setState');

      await component.instance()._getIntervals();

      expect(func.mock.calls[0][0]).toEqual({
        date: moment('2017-09-21').format(moment.HTML5_FMT.DATE),
        emails: ['test1@ya.ru', 'test3@yandex.ru', 'room1@yandex-team.ru'],
        exceptEventId: 111,
        shape: 'ids-only'
      });
      expect(TimelineWidgetContainer.prototype.setState).toHaveBeenCalledWith({
        intervals: {
          'test@yandex.ru': []
        },
        isLoading: false
      });
    });
  });

  describe('_getClippedEventStart', () => {
    test('должен вернуть начало дня от переданного start', () => {
      const component = setup();

      expect(component.instance()._getClippedEventStart(defaultProps)).toEqual(
        moment('2017-09-21').valueOf()
      );
    });
  });

  describe('_getResources', () => {
    test('должен вернуть список из участников с уникальными email', () => {
      const component = setup();

      expect(component.instance()._getResources(defaultProps)).toEqual([
        {
          email: 'test1@ya.ru',
          name: 'name1',
          login: 'login1'
        },
        {
          email: 'test3@yandex.ru',
          name: 'name3',
          login: 'login3'
        },
        {
          email: 'room1@yandex-team.ru',
          isRoomResource: true,
          name: 'name1'
        }
      ]);
    });

    test('должен вернуть одного учатсника, если он был единственным', () => {
      const component = setup();
      const props = {
        ...defaultProps,
        organizer: {
          email: 'test1@ya.ru',
          name: 'name1',
          login: 'login1'
        },
        members: [],
        resources: []
      };

      expect(component.instance()._getResources(props)).toEqual([
        {
          email: 'test1@ya.ru',
          name: 'name1',
          login: 'login1'
        }
      ]);
    });
  });

  describe('_getBookingStart', () => {
    test('должен вернуть начало дня, если isAllDay = true', () => {
      const component = setup({isAllDay: true});

      expect(component.instance()._getBookingStart()).toEqual(
        moment('2017-09-21')
          .startOf('day')
          .valueOf()
      );
    });

    test('должен вернуть start, если isAllDay = false', () => {
      const component = setup({isAllDay: false});

      expect(component.instance()._getBookingStart()).toEqual(
        moment('2017-09-21T15:01:00').valueOf()
      );
    });
  });

  describe('_getBookingEnd', () => {
    test('должен вернуть конец дня, если isAllDay = true', () => {
      const component = setup({isAllDay: true});

      expect(component.instance()._getBookingEnd()).toEqual(
        moment('2017-09-21')
          .endOf('day')
          .valueOf()
      );
    });

    test('должен вернуть end, если isAllDay = false', () => {
      const component = setup({isAllDay: false});

      expect(component.instance()._getBookingEnd()).toEqual(
        moment('2017-09-21T15:05:00').valueOf()
      );
    });
  });

  describe('_handlePrevClick', () => {
    test('должен записать в состояние -1 день и сбросить интервалы', () => {
      const component = setup();

      component.setState({intervals: {interval: 111}});
      component.instance()._handlePrevClick();
      expect(component.state()).toHaveProperty('timelineDate', moment('2017-09-20').valueOf());
      expect(component.state()).toHaveProperty('intervals', {});
    });
  });

  describe('_handleNextClick', () => {
    test('должен записать в состояние +1 день и сбросить интервалы', () => {
      const component = setup();

      component.setState({intervals: {interval: 111}});
      component.instance()._handleNextClick();
      expect(component.state()).toHaveProperty('timelineDate', moment('2017-09-22').valueOf());
      expect(component.state()).toHaveProperty('intervals', {});
    });
  });
});
