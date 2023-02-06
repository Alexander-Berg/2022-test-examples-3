import React from 'react';
import {mount} from 'enzyme';

import {RoomCard} from '../RoomCard';

function setup(props) {
  const actions = {
    getRoomInfo: jest.fn((_, resolve) => {
      resolve({email: props.email});
    })
  };
  const component = mount(
    <RoomCard trigger={<div className="trigger" />} {...props} {...actions} />
  );

  return {
    component,
    actions,
    trigger: component.find('.trigger')
  };
}

describe('<RoomCard />', () => {
  describe('handleTriggerMouseEnter', () => {
    beforeEach(() => {
      jest.spyOn(RoomCard.prototype, '_getRoomInfo').mockReturnValue();
    });

    test('должен вызвать загрузку данных, если навели на триггер, нет данных и не идет загрузка', () => {
      const {component, trigger} = setup({
        email: 'room@yandex-team.ru'
      });

      component.setState({
        isLoading: false,
        room: null
      });
      trigger.simulate('mouseEnter');

      expect(RoomCard.prototype._getRoomInfo).toHaveBeenCalledTimes(1);
    });

    test('не должен вызывать загрузку данных, если невали на триггер, но уже идет загрузка', () => {
      const {component, trigger} = setup({
        email: 'room@yandex-team.ru'
      });

      component.setState({
        isLoading: true,
        room: null
      });
      trigger.simulate('mouseEnter');

      expect(RoomCard.prototype._getRoomInfo).toHaveBeenCalledTimes(0);
    });

    test('не должен вызывать загрузку данных, если навели на триггер, но данные уже есть', () => {
      const {component, trigger} = setup({
        email: 'room@yandex-team.ru'
      });

      component.setState({
        isLoading: false,
        room: {
          email: 'room@yandex-team.ru'
        }
      });
      trigger.simulate('mouseEnter');

      expect(RoomCard.prototype._getRoomInfo).toHaveBeenCalledTimes(0);
    });
  });

  describe('getRoomInfo', () => {
    test('должен запросить по email данные для переговорки', async () => {
      const {component, actions} = setup({
        email: 'room@yandex-team.ru'
      });

      const request = component.instance()._getRoomInfo();

      expect(component.state('isLoading')).toBe(true);

      await request;

      expect(actions.getRoomInfo.mock.calls[0][0]).toEqual({
        email: 'room@yandex-team.ru'
      });
      expect(component.state()).toEqual({
        isLoading: false,
        room: {
          email: 'room@yandex-team.ru'
        }
      });
    });
  });

  describe('componentWillReceiveProps', () => {
    test('должен сбрасывать состояние, если email изменился', () => {
      const {component} = setup({
        email: 'room@yandex-team.ru'
      });

      component.setState({
        isLoading: false,
        room: {
          email: 'room@yandex-team.ru'
        }
      });
      component.setProps({
        email: 'room1@yandex-team.ru'
      });

      expect(component.state()).toEqual({
        isLoading: false,
        room: null
      });
    });

    test('не должен сбрасывать состояние, если email не изменился', () => {
      const {component} = setup({
        email: 'room@yandex-team.ru'
      });

      component.setState({
        isLoading: false,
        room: {
          email: 'room@yandex-team.ru'
        }
      });
      component.setProps({
        email: 'room@yandex-team.ru'
      });

      expect(component.state()).toEqual({
        isLoading: false,
        room: {
          email: 'room@yandex-team.ru'
        }
      });
    });
  });
});
