import { IRegion } from 'mg/types/apphost/region';

export const region: IRegion = {
  type: 'region',
  is_manual: false,
  default: {
    country_id: 225,
    country_iso_name: 'RU',
    id: 213,
    path: [213, 1, 3, 225, 10001, 10000],
  },
  lr: {
    country_id: 225,
    country_iso_name: 'RU',
    id: 213,
    path: [213, 1, 3, 225, 10001, 10000],
  },
  real: {
    country_id: 225,
    country_iso_name: 'RU',
    id: 54,
    path: [54, 121110, 11162, 52, 225, 10001, 10000],
  },
  tuned: {
    country_id: 225,
    country_iso_name: 'RU',
    id: 56,
    path: [56, 121115, 11225, 52, 225, 10001, 10000],
  },
};
