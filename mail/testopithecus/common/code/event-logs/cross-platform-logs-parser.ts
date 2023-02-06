import { JSONSerializer } from '../client/json/json-serializer'
import { Result } from '../client/result'
import { MapJSONItem, StringJSONItem } from '../mail/logging/json-types'
import { TestopithecusEvent } from '../mail/logging/testopithecus-event'
import { ValueMapBuilder } from '../mail/logging/value-map-builder'
import { requireNonNull } from '../utils/utils'

export class CrossPlatformLogsParser {
  constructor(private jsonSerializer: JSONSerializer) {
  }

  public parse(line: string): TestopithecusEvent {
    const json = this.jsonSerializer.deserialize(line, (j) => new Result(j, null)).getValue() as MapJSONItem
    const value = requireNonNull(json.getMap('value'), 'Нет аттрибутов у эвента!')
    const testopithecusName = (requireNonNull(value.get('event_name'), 'Имя евента должно быть в аттрибутах') as StringJSONItem).value
    const loggingName = requireNonNull(json.getString('name'), 'Имя эвента должно быть в имени эвента')
    if (loggingName !== `TESTOPITHECUS_EVENT_${testopithecusName}`) {
      throw new Error('Плохое имя')
    }
    return new TestopithecusEvent(testopithecusName, ValueMapBuilder.__parse(value))
  }
}
