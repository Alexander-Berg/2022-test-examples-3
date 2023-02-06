const langs = require.context('./', false, /\/.{2}\.tsx?$/);

export const testSendMaximumAddresses = langs(
  './' + process.env.PSTANKER_LANGPARAM + '.tsx'
).default as 'Можно указать не более 10 адресов';
