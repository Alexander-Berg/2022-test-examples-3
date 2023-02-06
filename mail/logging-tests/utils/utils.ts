import * as assert from 'assert';
import { Evaluation } from '../../../analytics/evaluations/evaluation';
import { ScenarioSplitter } from '../../../analytics/evaluations/scenario-splitting/scenario-splitter';
import { Scenario, ScenarioAttributes } from '../../../analytics/scenario';
import { MapAggregatorProvider } from '../../../code/mail/logging/agregation/aggregator-provider';
import { JSONItem} from '../../../code/mail/logging/json-types'
import { LoggingEvent, TestopithecusEvent } from '../../../code/mail/logging/testopithecus-event'
import { TestopithecusRegistry } from '../../../code/mail/logging/testopithecus-registry';
import { NativeTimeProvider, TimeProvider } from '../../../code/mail/logging/time-provider'
import { ValueMapBuilder } from '../../../code/mail/logging/value-map-builder'
import { Int32, int64, Int64 } from '../../../ys/ys'
import { JSONItemFromJSON } from '../../pod/json-helpers'
import { TestStackReporter } from '../reporting/test-stack-reporter';

export function setIncrementalTimeline(scenario: Scenario) {
  const times = []
  for (let i = 0; i < scenario.events.length; i++) {
    times.push(i)
  }
  setTimeline(scenario, times)
}

export function setTimeline(scenario: Scenario, times: Int32[]) {
  if (scenario.events.length !== times.length) {
    throw Error('Timeline size must have same size as scenario')
  }
  TestopithecusRegistry.timeProvider = new MockTimeProvider(times.map((t) => int64(t)))
  const reporter = new TestStackReporter()
  TestopithecusRegistry.setEventReporter(reporter)
  scenario.events.forEach((e, index) => {
    e.report()
  })
  TestopithecusRegistry.timeProvider = new NativeTimeProvider()
  scenario.events.splice(0, scenario.events.length)
  reporter.events.map((e) => parseEvent(e)).forEach((e) => scenario.thenEvent(e))
}

export function checkScenario(scenario: Scenario, expected: Scenario) {
  refreshProvider()
  const reporter = new TestStackReporter()
  for (const event of scenario.events) {
    event.reportVia(reporter)
  }
  const actualEvents = reporter.events.map((e) => {
    assert.strictEqual(e.attributes.has('timestamp'), true)
    e.attributes.delete('timestamp')
    return parseEvent(e)
  });
  assert.deepStrictEqual(actualEvents, expected.events)
}

export function checkEvaluationsResults<T, C>(evaluations: Array<Evaluation<T, C>>, actual: Map<string, any>, expected: any[]) {
  const results = collectResults(evaluations, actual)
  assert.deepStrictEqual(results, expected)
}

export function checkSplitterEvaluationResults<C>(evaluation: ScenarioSplitter<C>, actual: Map<string, ScenarioAttributes[]>, expected: any[][]) {
  const scenarioAttributes = actual.get(evaluation.name())!
  const evaluations = evaluation.getEvaluations()
  const result = []
  for (const scenarioAttribute of scenarioAttributes) {
    result.push(collectResults(evaluations, scenarioAttribute.attributes))
  }
  assert.deepStrictEqual(result, expected)
}

export function refreshProvider() {
  TestopithecusRegistry.setAggregatorProvider(new MapAggregatorProvider())
}

function collectResults<T, C>(evaluations: Array<Evaluation<T, C>>, actual: Map<string, any>) {
  const results = []
  for (const evaluation of evaluations) {
    results.push(actual.get(evaluation.name()))
  }
  return results
}

export function parseEvent(event: LoggingEvent): TestopithecusEvent {
  const attributes = new Map<string, JSONItem>()
  event.attributes.forEach((v, k) => {
    const parsedValue = JSONItemFromJSON(v)
    attributes.set(k, parsedValue)
  })
  return new TestopithecusEvent(event.name.replace('TESTOPITHECUS_EVENT_', ''), ValueMapBuilder.__parse(attributes))
}

export class MockTimeProvider implements TimeProvider {
  private position: Int32 = 0

  constructor(private times: Int64[]) {
  }

  public getCurrentTimeMs(): Int64 {
    return this.times[this.position++]
  }
}
