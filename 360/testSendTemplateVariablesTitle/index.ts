const langs = require.context('./', false, /\/.{2}\.tsx?$/);

export const testSendTemplateVariablesTitle = langs(
  './' + process.env.PSTANKER_LANGPARAM + '.tsx'
).default as 'Укажите значения переменных';
