/**
 * Copyright (c) Facebook, Inc. and its affiliates. All Rights Reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

// For some reason, doing `require`ing here works, while inside `cli` fails
// eslint-disable-next-line import/no-unresolved,@typescript-eslint/no-var-requires,global-require
export const VERSION: string = require('./package.json').version;
