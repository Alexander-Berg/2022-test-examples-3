import { EventReporter } from '../../../code/mail/logging/event-reporter'
import { LoggingEvent} from '../../../code/mail/logging/testopithecus-event'
import { TestopithecusRegistry } from '../../../code/mail/logging/testopithecus-registry'

export class TestStackReporter implements EventReporter {

  public events: LoggingEvent[] = []

  public report(event: LoggingEvent): void {
    this.events.push(event)
  }

}

export function initStackReporterInRegistry(): TestStackReporter {
  const reporter = new TestStackReporter()
  TestopithecusRegistry.setEventReporter(reporter)
  return reporter
}
