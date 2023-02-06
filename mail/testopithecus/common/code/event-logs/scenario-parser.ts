import { Int32 } from '../../ys/ys';
import { JSONSerializer } from '../client/json/json-serializer';
import { ArrayJSONItem, MapJSONItem } from '../mail/logging/json-types'
import { Result } from '../client/result';
import { TestPlan } from '../mbt/walk/fixed-scenario-strategy';
import { ActionParser } from './action-parser';

export class ScenarioParser {

  constructor(private actionParser: ActionParser, private jsonSerializer: JSONSerializer) { }

  public parse(eventStrings: string[]): TestPlan {
    const testPlan = TestPlan.empty()
    for (const event of eventStrings) {
      testPlan.then(this.actionParser.parseActionFromString(event))
    }
    return testPlan;
  }

  public parseFromJsonText(text: string): TestPlan {
    const actions = this.jsonSerializer.deserialize(text, (item) => new Result(item, null)).getValue() as ArrayJSONItem
    const testPlan = TestPlan.empty()
    for (const json of actions.asArray()) {
      testPlan.then(this.actionParser.parseActionFromJson(json as MapJSONItem))
    }
    return testPlan;
  }

  public parseFromText(text: string): TestPlan {
    const lines: string[] = [];
    for (const line of text.split('\n')) {
      lines.push(line.substring(line.search('{') as Int32, line.search('}') as Int32 + 1))
    }
    return this.parse(lines.filter((line) => line.length > 0))
  }

}
