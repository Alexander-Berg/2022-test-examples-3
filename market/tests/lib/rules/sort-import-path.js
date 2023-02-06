/**
 * @fileoverview Sorting imports by path
 * @author Alexander Ivankov
 */


//------------------------------------------------------------------------------
// Requirements
//------------------------------------------------------------------------------

const { RuleTester } = require('eslint');
const rule = require('../../../lib/rules/sort-import-path');

RuleTester.setDefaultConfig({
  parserOptions: {
    ecmaVersion: 6,
    sourceType: 'module',
  },
});


//------------------------------------------------------------------------------
// Tests
//------------------------------------------------------------------------------

const optionsAffiliate = [{
  projectFolders: ['^src/.*'],
  positionFullModuleImport: 'top',
  absolutePathSortOrder: [
    [
      'src/main/',
      'src/widgets-main/',
      'src/widget-type-.*/',
    ],

    [
      '/common/',
      '/client/',
      '/server/',
    ],
  ],

  packagesPathSortOrder: [
    [
      '^react$',
      '^classnames$',
    ],
  ],

}];

const ruleTester = new RuleTester();
ruleTester.run('sort-import-path', rule, {

  valid: [
    `
      import a from 'src/a';
      import b from 'src/b';
    `,

    // give me some code that won't trigger a warning
  ],

  invalid: [
    {
      code: `
      import b from 'src/b';
      import a from 'src/a';
      `,
      errors: [
        {
          message: 'Invalid imports paths sort',
          type: 'ImportDeclaration',
        },
      ],
      output: `
      import a from 'src/a';
      import b from 'src/b';
      `,
    },
    {
      code: `
      import {
        test,
        test1
      } from 'src/b';
      import a from 'src/a';
      `,
      errors: [
        {
          message: 'Invalid imports paths sort',
          type: 'ImportDeclaration',
        },
      ],
      output: `
      import a from 'src/a';
      import {
        test,
        test1
      } from 'src/b';
      `,
    },
    {
      code: `
      import {
        test,
        test1
      } from './../src/b';
      import a from '../../../zts/s';
      import top from 'src/a';
      `,
      errors: [
        {
          message: 'Invalid imports paths sort',
          type: 'ImportDeclaration',
        },
      ],
      output: `
      import top from 'src/a';
      import a from '../../../zts/s';
      import {
        test,
        test1
      } from './../src/b';
      `,
    },
    {
      code: `
      import wtm from 'src/widget-type-master/common/b';
      import {
        test,
        test1
      } from 'src/main/server/b';
      import a from 'src/main/client/a';
      import React from 'react';
      import wtt from 'src/widget-type-test/common/b';
      import wm from 'src/widgets-main/common/b';
      import top from 'src/main/common/b';
      `,
      options: optionsAffiliate,
      errors: [
        {
          message: 'Invalid imports paths sort',
          type: 'ImportDeclaration',
        },
      ],
      output: `
      import React from 'react';
      import top from 'src/main/common/b';
      import a from 'src/main/client/a';
      import {
        test,
        test1
      } from 'src/main/server/b';
      import wm from 'src/widgets-main/common/b';
      import wtm from 'src/widget-type-master/common/b';
      import wtt from 'src/widget-type-test/common/b';
      `,
    },
    {
      code: `
      import Widget from 'src/widgets-main/client/containers/Widget';
      import cn from 'classnames';
      import React from 'react';
      import assert from 'assert';
      import Button from '@yandex/market';
      import Header from '../Header';
      import path from 'path';
      import PromoLandingLink from '../PromoLandingLink';
      import CopyButton from '../../containers/CopyButton';
      import Text from '../Text';
      `,
      options: optionsAffiliate,
      errors: [
        {
          message: 'Invalid imports paths sort',
          type: 'ImportDeclaration',
        },
      ],
      output: `
      import assert from 'assert';
      import path from 'path';
      import React from 'react';
      import cn from 'classnames';
      import Button from '@yandex/market';
      import Widget from 'src/widgets-main/client/containers/Widget';
      import CopyButton from '../../containers/CopyButton';
      import Header from '../Header';
      import PromoLandingLink from '../PromoLandingLink';
      import Text from '../Text';
      `,
    },
    {
      code: `
      import React from 'react';
      import cn from 'classnames';
      import {upperFirst} from 'src/main/common/helpers/toolkit';
      import styles from './styles.module.css';
      import 'lego-on-react/b';
      import 'lego-on-react/a';
      import LegoTextInput from 'lego-on-react/es-modules-src/components/textinput/textinput.react';
      `,
      options: optionsAffiliate,
      errors: [
        {
          message: 'Invalid imports paths sort',
          type: 'ImportDeclaration',
        },
      ],
      output: `
      import 'lego-on-react/a';
      import 'lego-on-react/b';
      import React from 'react';
      import cn from 'classnames';
      import LegoTextInput from 'lego-on-react/es-modules-src/components/textinput/textinput.react';
      import {upperFirst} from 'src/main/common/helpers/toolkit';
      import styles from './styles.module.css';
      `,
    },
    {
      code: `
      import React from 'react';
      import 'lego-on-react/b';
      import 'lego-on-react/a';
      import cn from 'classnames';
      import {upperFirst} from 'src/main/common/helpers/toolkit';
      import styles from './styles.module.css';
      import LegoTextInput from 'lego-on-react/es-modules-src/components/textinput/textinput.react';
      `,
      options: [{
        ...optionsAffiliate[0],
        positionFullModuleImport: 'bottom',
      }],
      errors: [
        {
          message: 'Invalid imports paths sort',
          type: 'ImportDeclaration',
        },
      ],
      output: `
      import React from 'react';
      import cn from 'classnames';
      import LegoTextInput from 'lego-on-react/es-modules-src/components/textinput/textinput.react';
      import 'lego-on-react/a';
      import 'lego-on-react/b';
      import {upperFirst} from 'src/main/common/helpers/toolkit';
      import styles from './styles.module.css';
      `,
    },
    {
      code: `
      import {upperFirst} from '../a/b/index';
      import {upperSecond} from '../a/b/z';
      import styles from './styles.module.css';
      import baseStyles from '../styles.module.css';
      import {test} from './a/b/index';
      import {test1} from './a/b/z';
      import cn from '../../a/b/index';
      import React from '../../a/b/z';
      `,
      options: [{
        ...optionsAffiliate[0],
        relativePathSortOrder: [
          ['^(.(?!index$|styles))*$'],
        ],
      }],
      errors: [
        {
          message: 'Invalid imports paths sort',
          type: 'ImportDeclaration',
        },
      ],
      output: `
      import React from '../../a/b/z';
      import cn from '../../a/b/index';
      import {upperSecond} from '../a/b/z';
      import {upperFirst} from '../a/b/index';
      import baseStyles from '../styles.module.css';
      import {test1} from './a/b/z';
      import {test} from './a/b/index';
      import styles from './styles.module.css';
      `,
    },
    {
      code: `
      import {upperFirst} from '../a/b/index';
      import '../a/b/z';
      import styles from './styles.module.css';
      import baseStyles from '../styles.module.css';
      import {test} from './a/b/index';
      import '../a/b/index';
      import {test1} from './a/b/z';
      import {test3} from '..';
      import cn from '../../a/b/index';
      import React from '../../a/b/z';
      `,
      options: [{
        ...optionsAffiliate[0],
        relativePathSortOrder: [
          ['^(.(?!index$|styles))*$'],
        ],
      }],
      errors: [
        {
          message: 'Invalid imports paths sort',
          type: 'ImportDeclaration',
        },
      ],
      output: `
      import '../a/b/z';
      import '../a/b/index';
      import React from '../../a/b/z';
      import cn from '../../a/b/index';
      import {test3} from '..';
      import {upperFirst} from '../a/b/index';
      import baseStyles from '../styles.module.css';
      import {test1} from './a/b/z';
      import {test} from './a/b/index';
      import styles from './styles.module.css';
      `,
    },
  ],
});
