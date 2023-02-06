const langs = require.context('./', false, /\/.{2}\.tsx?$/);

export const testSendCantSend = langs(
  './' + process.env.PSTANKER_LANGPARAM + '.tsx'
)
  .default as 'Не удалось отправить тестовое письмо, проверьте значения на странице';
