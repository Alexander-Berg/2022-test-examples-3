/* tslint:disable:no-string-literal variable-name no-console */
import * as readline from 'readline'
import { AnalyticsRunner } from '../analytics/analytics-runner'
import { Evaluation } from '../analytics/evaluations/evaluation';
import { MailContext } from '../analytics/mail/mail-context';
import { MailContextApplier } from '../analytics/mail/mail-context-applier';
import { DefaultScenarios } from '../analytics/mail/scenarios/default-scenarios';
import { Scenario, ScenarioAttributes } from '../analytics/scenario'
import { LoggingEvent} from '../code/mail/logging/testopithecus-event'
import { range } from '../ys/ys'
import { parseEvent } from './logging-tests/utils/utils'

describe('Should run evaluation over YT', () => {
  it('first-event and session-length', () => {
    const rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout,
      terminal: false,
    })

    rl.on('line', (line) => {
      const json_data = JSON.parse(line)
      EvaluationsExport.EXPORTER.setData(json_data)
      const json_scenario = json_data['Scenario']
      const scenario = new Scenario()
      for (const event of json_scenario) {
        const values = new Map<string, any>()
        const json_event = JSON.parse(event)
        for (const field of Object.keys(json_event)) {
          values.set(field, json_event[field])
        }
        const eventName = 'TESTOPITHECUS_EVENT_' + values.get('event_name')
        const loggedEvent = new LoggingEvent(eventName, values)
        const parsedEvent = parseEvent(loggedEvent)
        scenario.thenEvent(parsedEvent)
      }
      evaluate(scenario)
    })
  });
});

function evaluate(scenario: Scenario) {
  const runner = new AnalyticsRunner()
  const evaluations: Array<Evaluation<ScenarioAttributes[], MailContext>> = [
    // DefaultScenarios.globalScenario(),
    DefaultScenarios.composeScenario(),
  ];
  const scenarios: Map<string, ScenarioAttributes[]> = runner.evaluateWithContext(scenario, evaluations, new MailContextApplier());
  for (const name of scenarios.keys()) {
    const names: string[] = ['name', 'attributes']
    const scenarioAttributes = scenarios.get(name)!
    for (const attributes of scenarioAttributes) {
      const result = {};
      // @ts-ignore
      attributes.attributes.forEach((value, key) => result[key] = value)
      EvaluationsExport.EXPORTER.export(names, [name, result])
    }
  }
}

class EvaluationsExport {
  public static EXPORTER = new EvaluationsExport()
  private data: {[key: string]: any} = {}

  private constructor() {}

  public setData(data: {[key: string]: any}): void {
    this.data = data
  }

  public export(keys: string[], values: any[]): void {
    let str = '{'
    for (const field in this.data) {
      if (field !== 'Scenario') {
        str += `"${field}": ${stringify(this.data[field])}`
        if (keys.length > 0) {
          str += `,`
        }
      }
    }
    for (const i of range(0, keys.length)) {
      str += `"${keys[i]}": ${stringify(values[i])}`
      if (i < keys.length - 1) {
        str += `,`
      }
    }
    str += '}'
    console.log(str)
  }
}

function stringify(obj: any): string {
  return JSON.stringify(obj, (key, value) => typeof value === 'bigint' ? value.toString() : value)
}
