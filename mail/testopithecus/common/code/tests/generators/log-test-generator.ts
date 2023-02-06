import { JSONSerializer } from '../../client/json/json-serializer';
import { ActionParser } from '../../event-logs/action-parser';
import { LogGeneratedTest } from '../../event-logs/log-generated-test';
import { ScenarioParser } from '../../event-logs/scenario-parser';
import { MBTTest } from '../../mbt/mbt-test'
import { MBTTestGenerator } from './mbt-test-generator';

export class LogTestGenerator implements MBTTestGenerator {

  private rawLogs: string[] = []
  private jsonLogs: string[] = []

  private scenarioParser: ScenarioParser

  constructor(private jsonSerializer: JSONSerializer) {
    this.scenarioParser = new ScenarioParser(new ActionParser(), this.jsonSerializer)
  }

  public generateTests(): MBTTest[] {
    const tests: MBTTest[] = [];
    for (const log of this.rawLogs) {
      tests.push(new LogGeneratedTest(this.scenarioParser.parseFromText(log)))
    }
    for (const log of this.jsonLogs) {
      tests.push(new LogGeneratedTest(this.scenarioParser.parseFromJsonText(log)))
    }
    return tests
  }

}
