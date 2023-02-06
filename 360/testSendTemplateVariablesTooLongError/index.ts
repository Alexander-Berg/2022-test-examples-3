import type { TTestSendTemplateVariablesTooLongError } from './types';

const langs = require.context('./', false, /\/.{2}\.tsx?$/);

export const testSendTemplateVariablesTooLongError = langs(
  './' + process.env.PSTANKER_LANGPARAM + '.tsx'
).default as TTestSendTemplateVariablesTooLongError;
