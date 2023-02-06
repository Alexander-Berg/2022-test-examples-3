import {TestScheduler} from 'rxjs/testing'

export default function createScheduler() {
  return new TestScheduler((actual, expected) => expect(actual).toEqual(expected))
}
