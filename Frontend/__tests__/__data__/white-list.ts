import { EPlatform } from '../types';
import { EService } from '../types';

enum EPage {
  STORY = 'story',
  MAIN = 'main',
  RUBRIC = 'rubric',
}

type TPageWithFeed = EPage.STORY | EPage.RUBRIC;

export type TFlagsDeclaration = [
  [
    `yxneo_${EService.NEWS}_${EPlatform.PHONE}_${EPage.STORY}_my-awesome-feature`,
    {
      description: 'ОК',
      values: '0|1',
      task: 'NERPA-XXXXX',
      type: boolean,
    },
  ],
  [
    `yxneo_${EService.NEWS}_${TPageWithFeed}_my-awesome-feature`,
    {
      description: 'ОК',
      values: '0|1',
      task: 'NERPA-XXXXX',
      type: boolean,
    },
  ],
];
