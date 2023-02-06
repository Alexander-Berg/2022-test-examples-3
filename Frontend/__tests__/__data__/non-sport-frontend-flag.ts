/*
 * Проверяем не спортивные(!yxneo) флаги и флаги бекенда
 */

export type TFlagsDeclaration = [
  [
    'tabs_order_version',
    {
      description: 'ОК',
      values: '0|1',
      task: 'NERPA-XXXXX',
      type: boolean,
    },
  ],
  [
    'yxnews_reset_hsts',
    {
      description: 'ОК',
      values: '0|1',
      task: 'NERPA-XXXXX',
      type: boolean,
    },
  ],
];
