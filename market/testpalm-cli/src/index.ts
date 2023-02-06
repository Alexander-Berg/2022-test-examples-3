import request from 'axios'
import chalk from 'chalk'
import moment from 'moment'
import dedent from 'dedent-js'
import invariant from 'invariant'
// @ts-ignore
const TestpalmClient = require('@yandex-int/testpalm-api').default

import { pipe, groupBy, mapObjIndexed } from 'ramda'
import snakeCase from 'snake-case'

import { parseArgv } from './argv'
import { mapKeys } from './utils'

const TESTPALM_OAUTH_API_TOKEN = process.env.TESTPALM_OAUTH_API_TOKEN
const STATFACE_API_TOKEN = process.env.STATFACE_API_TOKEN

process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'

const catchError = <T>(fn: () => Promise<T>) => () => fn().catch(console.error)

const usage = 'Usage: $0 <projectId>'

const byStatus = pipe(
  groupBy(({ status }) => status),
  mapObjIndexed((testcases: any[]) => testcases.length),
)

const byAutotest = (testCases: any[]) => ({
  total: testCases.length,
  isAutotest: testCases.filter(({ isAutotest }) => isAutotest).length,
})

const normalizeKeys = mapKeys(snakeCase)

const formats = {
  text: console.log,
  json: (v: any) => console.log(JSON.stringify(v, null, 2)),
}

const printResult = (format: keyof typeof formats = 'text') => formats[format]

const testpalmClient = new TestpalmClient(TESTPALM_OAUTH_API_TOKEN)

const getTestPalmStat = (projectId: string) =>
  testpalmClient
    .getTestCases(projectId)
    .then((testCases: any[]) =>
      normalizeKeys({
        ...byAutotest(testCases),
        ...byStatus(testCases),
      }),
    )
    .then(stat => [{ fielddate: moment().format('DD.MM.YYYY'), ...stat }])

const getTestPalmRuns = async (projectId: string) => {
  const from = moment()
    .subtract(1, 'day')
    .startOf('day')
    .valueOf()
  const to = moment()
    .startOf('day')
    .valueOf()

  return testpalmClient
    .getTestRuns(projectId, {
      expression: JSON.stringify({
        type: 'AND',
        left: {
          type: 'AND',
          left: { type: 'GT', key: 'finishedTime', value: from },
          right: { type: 'LT', key: 'finishedTime', value: to },
        },
        right: {
          type: 'AND',
          left: {
            type: 'AND',
            left: {
              type: 'CONTAIN',
              key: 'title',
              value: 'testing-hermione-only',
            },
            right: {
              type: 'AND',
              left: {
                type: 'NOT_CONTAIN',
                key: 'title',
                value: 'broken',
              },
              right: {
                type: 'AND',
                left: {
                  type: 'NOT_CONTAIN',
                  key: 'title',
                  value: 'auto-filter',
                },
                right: {
                  type: 'NOT_CONTAIN',
                  key: 'title',
                  value: 'case-filter',
                },
              },
            },
          },
          right: {
            type: 'EQ',
            key: 'status',
            value: 'FINISHED',
          },
        },
      }),
      limit: 1,
      createdTimeSort: 'desc',
      include: [
        'executionTime',
        'finishedTime',
        'startedTime',
        'resolution',
        'version',
        'title',
      ],
    })
    .then((testRuns: any[]) =>
      testRuns.map(run => {
        const {
          version,
          startedTime,
          finishedTime,
          executionTime,
          resolution,
        } = run
        let start = moment(startedTime)
        let end = moment(finishedTime)
        let diff = end.diff(start)

        let formattedExecutionTime = moment
          .utc(diff)
          .format('HH ч. mm м. ss с.')

        return {
          version,
          autotests_count: resolution.counter.total,
          execution_time: executionTime,
          formatted_execution_time: formattedExecutionTime,
          fielddatetime: moment(startedTime).format('YYYY-MM-DD HH:MM:00'),
        }
      }),
    )
}

const postToStatface = name => values => {
  invariant(
    STATFACE_API_TOKEN,
    'Env variable "STATFACE_API_TOKEN" is required with --statface option.',
  )

  return request
    .post(
      `https://upload.stat.yandex-team.ru/_api/report/data/${name}`,
      {
        scale: 'd',
        json_data: JSON.stringify({ values }),
        _append_mode: 1,
      },
      {
        headers: {
          Authorization: `OAuth ${STATFACE_API_TOKEN}`,
        },
      },
    )
    .then(r => r.data)
    .then(r => (r && r.message ? r.message : r))
}

export const run = catchError(async () => {
  const argv = await parseArgv({
    usage,
    options: ['statface', 'verbose', 'command'],
    check: ({ _: args, $0 }) => {
      if (!args[0]) {
        throw new Error(dedent`
          ${chalk.red('The <projectId> option is required')}
          ${usage
            .replace('$0', $0)
            .replace('<trackerVersion>', chalk.red('<trackerVersion>'))}
        `)
      }
    },
  })
  const { _: args, statface: statfaceReportName, command, verbose } = argv
  const [projectId] = args

  invariant(
    TESTPALM_OAUTH_API_TOKEN,
    'Env variable "TESTPALM_OAUTH_API_TOKEN" is required',
  )

  // debug && console.log('Options:', argv)
  // verbose && console.log('"--verbose" option passed')

  return (command === 'testruns'
    ? getTestPalmRuns(projectId)
    : getTestPalmStat(projectId)
  )
    .then(
      // Если нужно вывести и данные и сообщение об отправке данных
      verbose && statfaceReportName ? printResult('json') : v => v,
    )
    .then(
      statfaceReportName
        ? postToStatface(statfaceReportName)
        : projectId => projectId,
    )
    .then(printResult(statfaceReportName ? 'text' : 'json'))
    .catch(console.error)
})
