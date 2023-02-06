const langs = require.context('./', false, /\/.{2}\.tsx?$/);

export const testSendInvalidAddress = langs(
  './' + process.env.PSTANKER_LANGPARAM + '.tsx'
).default as 'Некорректный email-адрес';
