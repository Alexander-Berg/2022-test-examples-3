import {TestCollection} from 'hermione'
import {Testpalm} from './Testpalm'
import getBrowsers from '../../getBrowsers'

type PluginOptions = {
  enabled: boolean
  testpalmToken: string
  project: string
}

module.exports = (hermione: Hermione.Process, options: PluginOptions) => {
  const {enabled} = options

  if (!enabled) {
    return
  }

  const testpalm = new Testpalm({
    token: options.testpalmToken,
    browsers: Object.keys(getBrowsers()),
    project: options.project
  })

  if (!hermione.isWorker()) {
    hermione.on(hermione.events.INIT, async () => {
      console.log('[TESTPALM-SKIP] Plugin enabled. Loading skips')
      await testpalm.fetchSkips()
    })
  }

  hermione.on(hermione.events.AFTER_TESTS_READ, (testCollection: TestCollection) => {
    testCollection.eachTest((test, browserId) => {
      const skipReason = testpalm.isSkipped(test.title, browserId)
      if (skipReason) {
        test.pending = true
        // @ts-ignore
        test.skipReason = skipReason
      }
    })
  })
}
