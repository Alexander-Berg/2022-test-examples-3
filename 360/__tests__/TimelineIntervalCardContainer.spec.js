import React from 'react';
import {mount} from 'enzyme';
import moment from 'moment';

import {TimelineIntervalCardContainer} from '../TimelineIntervalCardContainer';

function setup(props) {
  const actions = {
    getEventsInfo: jest.fn((_, resolve) => {
      resolve(['111', '222']);
    })
  };
  const component = mount(
    <TimelineIntervalCardContainer
      trigger={<div className="trigger" />}
      timelineDate={moment('2017-09-21').valueOf()}
      start={moment('2017-09-21T11:00:00').valueOf()}
      end={moment('2017-09-21T12:00:00').valueOf()}
      {...props}
      {...actions}
    />
  );

  return {
    component,
    actions,
    trigger: component.find('.trigger')
  };
}

describe('<TimelineIntervalCardContainer />', () => {
  describe('handleTriggerMouseEnter', () => {
    beforeEach(() => {
      jest.spyOn(TimelineIntervalCardContainer.prototype, '_getEventsInfo').mockReturnValue();
    });

    test('должен вызвать загрузку данных, если навели на триггер, нет данных и не идет загрузка', () => {
      const {component, trigger} = setup({
        eventIds: [1, 2, 3],
        forResource: true
      });
      component.setState({
        isLoading: false,
        events: null
      });
      trigger.simulate('mouseEnter');
      expect(TimelineIntervalCardContainer.prototype._getEventsInfo).toHaveBeenCalledTimes(1);
    });

    test('не должен вызывать загрузку данных, если невали на триггер, но уже идет загрузка', () => {
      const {component, trigger} = setup({
        eventIds: [1, 2, 3],
        forResource: true
      });

      component.setState({
        isLoading: true,
        events: null
      });
      trigger.simulate('mouseEnter');

      expect(TimelineIntervalCardContainer.prototype._getEventsInfo).toHaveBeenCalledTimes(0);
    });

    test('не должен вызывать загрузку данных, если навели на триггер, но данные уже есть', () => {
      const {component, trigger} = setup({
        eventIds: [1, 2, 3],
        forResource: true
      });

      component.setState({
        isLoading: false,
        events: [{attendees: [], id: 1}]
      });
      trigger.simulate('mouseEnter');

      expect(TimelineIntervalCardContainer.prototype._getEventsInfo).toHaveBeenCalledTimes(0);
    });
  });
  describe('getEventsInfo', () => {
    test('должен запросить по eventIds данные для событий', async () => {
      const {component, actions} = setup({
        eventIds: [1, 2, 3],
        forResource: true
      });
      const request = component.instance()._getEventsInfo();

      expect(component.state('isLoading')).toBe(true);

      await request;

      expect(actions.getEventsInfo.mock.calls[0][0]).toEqual({
        forResource: true,
        eventIds: [1, 2, 3]
      });
      expect(component.state()).toEqual({
        isLoading: false,
        events: ['111', '222']
      });
    });
  });
});
