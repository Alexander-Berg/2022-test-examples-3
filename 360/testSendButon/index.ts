const langs = require.context('./', false, /\/.{2}\.tsx?$/);

export const testSendButon = langs(
  './' + process.env.PSTANKER_LANGPARAM + '.tsx'
).default as 'Тестовое письмо';
