const testRunIdUtils = require('@yandex-int/test-run-id-utils')

module.exports = () => ({
  'pre-data-fetch': (ctx, next) => {
    const testRunId = ctx.req.parsedURL.query.testRunId || ctx.req.cookies.testRunId

    if (!testRunId) {
      return next(new Error('Отсутствует обязательный query параметр "testRunId" или кука "testRunId"'))
    }

    // Удаляем служебные парамеры из запросов в бекенд
    const removeQueryParams = ['tpid', 'testRunId']
    for (const key of removeQueryParams) {
      delete ctx.sourceReq.query[key]
    }

    ctx.testRunData = testRunIdUtils.parse(testRunId)
    next()
  }
})
