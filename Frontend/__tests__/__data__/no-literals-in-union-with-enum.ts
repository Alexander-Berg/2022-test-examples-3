import { EPlatform } from '../types';

export type TFlagsDeclaration = [
  [
    `yxneo_${EPlatform.DESKTOP | 'tablet'}_my-awesome-feature`,
    {
      description: '!ОК - union из enum и литерала',
      values: '0|1',
      task: 'NERPA-XXXXX',
      type: boolean,
    },
  ],
];
