import { ESemanticPolicy, TFlags } from 'neo/types/flags';
import { IFlagPossibleValue } from '../../getPageFlags';

export const policies = {
  [ESemanticPolicy.SERVICE]: ['sport', 'news'],
  [ESemanticPolicy.PLATFORM]: ['phone', 'desktop'],
  [ESemanticPolicy.PAGE]: ['main', 'story'],
  [ESemanticPolicy.LITERAL]: [],
};

interface ITest {
  name: string;
  expected: IFlagPossibleValue[];
  flagTemplate: TFlags;
}

export const tests: ITest[] = [
  {
    name: 'Флаг с несколькими стратегиями(enum)',
    expected: [
      {
        name: 'yxneo_news_desktop_story_flag',
        hashObject: { service: 'news', platform: 'desktop', page: 'story' },
      },
      {
        name: 'yxneo_news_phone_story_flag',
        hashObject: { service: 'news', platform: 'phone', page: 'story' },
      },
    ],
    flagTemplate: {
      'yxneo_${EService.NEWS}_${EPlatform}_${EPage.STORY}_flag': {
        template: 'yxneo_{0}_{1}_{2}_flag',
        params: [
          {
            name: 'EService',
            policy: ESemanticPolicy.SERVICE,
            type: 'select',
            options: [
              'news',
            ],
          },
          {
            name: 'EPlatform',
            policy: ESemanticPolicy.PLATFORM,
            type: 'select',
            options: [
              'desktop',
              'phone',
            ],
          },
          {
            name: 'EPage',
            policy: ESemanticPolicy.PAGE,
            type: 'select',
            options: [
              'story',
            ],
          },
        ],
        type: 'number',
        description: 'description',
        values: '1|2|3|4|5',
        task: 'NERPA-xxxxx',
      },
    },
  },
  {
    name: 'Флаг без стратегий',
    expected: [
      {
        name: 'yxneo_flag',
        hashObject: { service: null, platform: null, page: null },
      },
    ],
    flagTemplate: {
      yxneo_flag: {
        template: 'yxneo_flag',
        params: [],
        type: 'boolean',
        description: 'description',
        values: '0|1',
        task: 'NERPA-xxxxx',
      },
    },
  },
  {
    name: 'Флаг c литеральными стратегиями (TPage)',
    expected: [
      {
        name: 'yxneo_news_main_flag',
        hashObject: { service: 'news', platform: null, page: 'main' },
      },
      {
        name: 'yxneo_sport_main_flag',
        hashObject: { service: 'sport', platform: null, page: 'main' },
      },
      {
        name: 'yxneo_news_story_flag',
        hashObject: { service: 'news', platform: null, page: 'story' },
      },
      {
        name: 'yxneo_sport_story_flag',
        hashObject: { service: 'sport', platform: null, page: 'story' },
      },
    ],
    flagTemplate: {
      'yxneo_${EService}_${TPage}_flag': {
        template: 'yxneo_{0}_{1}_flag',
        params: [
          {
            name: 'EService',
            policy: ESemanticPolicy.SERVICE,
            type: 'select',
            options: [
              'news',
              'sport',
            ],
          },
          {
            name: 'TPage',
            policy: ESemanticPolicy.PAGE,
            type: 'string',
          },
        ],
        type: 'number',
        description: 'description',
        values: '1|2|3|4|5',
        task: 'NERPA-xxxxx',
      },
    },
  },
];
