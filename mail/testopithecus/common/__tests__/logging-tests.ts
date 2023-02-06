import * as assert from 'assert';
import { EventReporter } from '../code/mail/logging/event-reporter';
import { Testopithecus } from '../code/mail/logging/events/testopithecus';
import { LoggingEvent, TestopithecusEvent } from '../code/mail/logging/testopithecus-event'
import { Nullable } from '../ys/ys';

class MockReporter implements EventReporter {
  public lastEvent: Nullable<LoggingEvent> = null

  public report(event: LoggingEvent): void {
    this.lastEvent = event
  }
}

describe('Testopithecus events', () => {
  it('should log timestamp in ms', (done) => {
    const currentTimeInMs = Date.now()
    const reporter = new MockReporter()
    Testopithecus.startEvents.startWithMessageListShow().reportVia(reporter)
    const timestamp = reporter.lastEvent!.attributes.get('timestamp')
    console.log(timestamp)
    assert.ok(timestamp >= currentTimeInMs)
    done()
  })
})
