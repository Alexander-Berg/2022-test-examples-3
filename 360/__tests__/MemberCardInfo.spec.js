import React from 'react';
import {shallow} from 'enzyme';

import MemberCardInfo from '../MemberCardInfo';

jest.mock('utils/i18n');

const baseData = {
  first_name: 'first_name',
  last_name: 'last_name',
  staff_url: '//staff.yandex-team.ru/username',
  login: 'tavria'
};
const mainData = {
  avatar: '//center.yandex-team.ru/user/avatar/username/100/square',
  position: 'position',
  dep: {
    name: 'dep_name'
  },
  work_email: 'username@yandex-team.ru',
  phones: {
    work: '1234',
    mobile: '+71234567890'
  },
  office: 'office',
  last_office: {
    ago: 'last_office_ago',
    office: 'last_office'
  },
  gap: {
    id: 58595157,
    color: '#73c9eb',
    caption: 'ggwp',
    externalId: '1650103gap.yandex-team.ru',
    layerId: 26506,
    type: 'vacation',
    name: 'Отпуск',
    startTs: '2021-10-02T00:00:00',
    endTs: '2021-10-18T00:00:00',
    isAllDay: true,
    decision: 'yes',
    availability: 'busy'
  }
};

describe('<MemberCardInfo />', () => {
  describe('режим загрузки', () => {
    test('должен показывать спиннер вместо основного контента', () => {
      const component = shallow(<MemberCardInfo isLoading member={null} />);

      expect(component).toMatchSnapshot();
    });
  });

  describe('обычный режим', () => {
    test('должен показывать основной контект вместо спиннера', () => {
      const component = shallow(<MemberCardInfo isLoading={false} member={baseData} />);

      expect(component).toMatchSnapshot();
    });

    test('должен показывать аватар', () => {
      const component = shallow(
        <MemberCardInfo
          isLoading={false}
          member={{
            ...baseData,
            avatar: mainData.avatar
          }}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать позицию', () => {
      const component = shallow(
        <MemberCardInfo
          isLoading={false}
          member={{
            ...baseData,
            position: mainData.position
          }}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать подразделение', () => {
      const component = shallow(
        <MemberCardInfo
          isLoading={false}
          member={{
            ...baseData,
            dep: mainData.dep
          }}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать рабочую почту', () => {
      const component = shallow(
        <MemberCardInfo
          isLoading={false}
          member={{
            ...baseData,
            work_email: mainData.work_email
          }}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать рабочий телефон', () => {
      const component = shallow(
        <MemberCardInfo
          isLoading={false}
          member={{
            ...baseData,
            phones: {work: mainData.phones.work}
          }}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать мобильный телефон', () => {
      const component = shallow(
        <MemberCardInfo
          isLoading={false}
          member={{
            ...baseData,
            phones: {mobile: mainData.phones.mobile}
          }}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать рабочее место', () => {
      const component = shallow(
        <MemberCardInfo
          isLoading={false}
          member={{
            ...baseData,
            office: mainData.office
          }}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать последнее рабочее место', () => {
      const component = shallow(
        <MemberCardInfo
          isLoading={false}
          member={{
            ...baseData,
            last_office: mainData.last_office
          }}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать последнее рабочее место для девушки', () => {
      const component = shallow(
        <MemberCardInfo
          isLoading={false}
          member={{
            ...baseData,
            last_office: mainData.last_office,
            gender: 'F'
          }}
        />
      );

      expect(component).toMatchSnapshot();
    });

    test('должен показывать коммандировки/отсутствия', () => {
      const component = shallow(
        <MemberCardInfo
          isLoading={false}
          member={{
            ...baseData
          }}
          gap={mainData.gap}
        />
      );

      expect(component).toMatchSnapshot();
    });
  });
});
