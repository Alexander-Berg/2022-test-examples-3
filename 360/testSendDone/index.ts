const langs = require.context('./', false, /\/.{2}\.tsx?$/);

export const testSendDone = langs(
  './' + process.env.PSTANKER_LANGPARAM + '.tsx'
).default as 'Тестовая рассылка отправлена';
