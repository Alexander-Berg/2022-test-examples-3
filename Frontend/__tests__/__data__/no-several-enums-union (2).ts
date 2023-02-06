import { EPlatform } from '../types';
import { EService } from '../types';

export type TFlagsDeclaration = [
  [
    `yxneo_${EService.SPORT | EPlatform.PHONE}_my-awesome-feature`,
    {
      description: '!ОК - объединение нескольких перечислений',
      values: '0|1',
      task: 'NERPA-XXXXX',
      type: boolean,
    },
  ],
];
