import { DoctorEntities, DoctorViewType } from 'src/entities/doctor/types';

const links = [
  {
    url: 'https://doctor.market.yandex-team.ru',
    text: 'Доктор',
  },
];

const linksSection = {
  type: DoctorViewType.LINKS,
  expanded: true,
};

export const GlobalLinks: DoctorEntities = {
  sections: [linksSection],
  links,
};

export const LocalLinks: DoctorEntities = {
  sections: [
    {
      ...linksSection,
      value: links,
    },
  ],
};

const checks = [
  {
    code: '49',
    message: 'Error',
    level: 'ERROR',
  },
];

const checkSection = {
  type: DoctorViewType.CHECKS,
  expanded: true,
};

export const GlobalChecks: DoctorEntities = {
  sections: [checkSection],
  checks,
};

export const LocalChecks: DoctorEntities = {
  sections: [{ ...checkSection, value: checks }],
};

const timings = [
  {
    title: 'Парсинг офера',
    timestamp: 1600000,
    source: 'MBO',
  },
];

const timingsSection = {
  type: DoctorViewType.TIMINGS,
  expanded: true,
};

export const GlobalTimings: DoctorEntities = {
  sections: [timingsSection],
  timings,
};

export const LocalTimings: DoctorEntities = {
  sections: [{ ...timingsSection, value: timings }],
};

export const KeyValue: DoctorEntities = {
  sections: [
    {
      title: 'Идентификаторы офера',
      type: DoctorViewType.KEY_VALUE,
      expanded: true,
      value: [
        { key: 'shopSku', value: 869 },
        {
          key: 'bussinesId',
          value: '888888',
        },
      ],
    },
  ],
};

export const Json: DoctorEntities = {
  sections: [
    {
      title: 'Офер',
      type: DoctorViewType.JSON,
      expanded: true,
      value: {
        name: 'json',
        formats: ['js', 'ts'],
      },
    },
  ],
};

export const doctorResponse: DoctorEntities = {
  sections: [linksSection, checkSection, timingsSection],
  links,
  checks,
};
