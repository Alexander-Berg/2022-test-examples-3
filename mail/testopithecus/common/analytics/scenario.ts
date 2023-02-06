import { TestopithecusEvent } from '../code/mail/logging/testopithecus-event';

export class Scenario {

  constructor(public readonly events: TestopithecusEvent[] = []) {}

  public thenEvent(event: TestopithecusEvent): Scenario {
    this.events.push(event)
    return this
  }

}

export class ScenarioAttributes {

  constructor(public readonly attributes: Map<string, any> = new Map()) {}

  public setAttribute(name: string, value: any): ScenarioAttributes {
    this.attributes.set(name, value)
    return this
  }

  public addAttributes(attributes: ScenarioAttributes) {
    for (const key of attributes.attributes.keys()) {
      // @ts-ignore
      this.setAttribute(key, attributes.attributes.get(key))
    }
  }

}
