const langs = require.context('./', false, /\/.{2}\.tsx?$/);

export const testSendNoEmails = langs(
  './' + process.env.PSTANKER_LANGPARAM + '.tsx'
).default as 'Не указаны адреса получателей';
