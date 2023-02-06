import React from 'react';
import {shallow} from 'enzyme';

import RoomCardInfo from '../RoomCardInfo';

jest.mock('utils/i18n');

describe('<RoomCardInfo />', () => {
  describe('режим загрузки', () => {
    test('должен показывать спиннер вместо основного контента', () => {
      const component = shallow(<RoomCardInfo isLoading room={null} />);

      expect(component).toMatchSnapshot();
    });
  });

  describe('обычный режим', () => {
    test('должен показывать основной контект вместо спиннера', () => {
      const component = shallow(
        <RoomCardInfo
          isLoading={false}
          room={{
            email: 'room@yandex-team.ru',
            name: 'roomName'
          }}
          features={{}}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать вместимость', () => {
      const component = shallow(
        <RoomCardInfo
          isLoading={false}
          room={{
            capacity: 10,
            email: 'room@yandex-team.ru',
            name: 'roomName'
          }}
          features={{enableResourceMap: true}}
          isSpaceshipActivated
          isFullForm
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать количество стульев', () => {
      const component = shallow(
        <RoomCardInfo
          isLoading={false}
          room={{
            capacity: 10,
            seats: 10,
            email: 'room@yandex-team.ru',
            name: 'roomName'
          }}
          features={{}}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать телефон, если он есть', () => {
      const component = shallow(
        <RoomCardInfo
          isLoading={false}
          room={{
            phone: 1030,
            email: 'room@yandex-team.ru',
            name: 'roomName'
          }}
          features={{}}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать видео-связь, если она есть', () => {
      const component = shallow(
        <RoomCardInfo
          isLoading={false}
          room={{
            video: 1030,
            email: 'room@yandex-team.ru',
            name: 'roomName'
          }}
          features={{}}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать LCD монитор, если он есть', () => {
      const component = shallow(
        <RoomCardInfo
          isLoading={false}
          room={{
            lcdPanel: true,
            email: 'room@yandex-team.ru',
            name: 'roomName'
          }}
          features={{}}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать проектор, если он есть', () => {
      const component = shallow(
        <RoomCardInfo
          isLoading={false}
          room={{
            projector: true,
            email: 'room@yandex-team.ru',
            name: 'roomName'
          }}
          features={{}}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать маркерную доску, если она есть', () => {
      const component = shallow(
        <RoomCardInfo
          isLoading={false}
          room={{
            markerBoard: true,
            email: 'room@yandex-team.ru',
            name: 'roomName'
          }}
          features={{}}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать гостевой wifi, если он есть', () => {
      const component = shallow(
        <RoomCardInfo
          isLoading={false}
          room={{
            guestWifi: true,
            email: 'room@yandex-team.ru',
            name: 'roomName'
          }}
          features={{}}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать описание переговорки, если оно есть', () => {
      const component = shallow(
        <RoomCardInfo
          isLoading={false}
          room={{
            description: 'description',
            email: 'room@yandex-team.ru',
            name: 'roomName'
          }}
          features={{}}
        />
      );

      expect(component).toMatchSnapshot();
    });
  });
});
