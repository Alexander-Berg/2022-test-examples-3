const langs = require.context('./', false, /\/.{2}\.tsx?$/);

export const testSendTemplateVariablesDesc = langs(
  './' + process.env.PSTANKER_LANGPARAM + '.tsx'
).default as 'Эти значения подставятся для всех получателей тестового письма.';
