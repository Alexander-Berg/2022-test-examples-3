import { Nullable, undefinedToNull } from '../../../../ys/ys';
import { EventNames } from '../events/event-names';
import { TestopithecusEvent } from '../testopithecus-event';
import { MessageListSyncAggregator } from './aggragators/message-list-sync-aggregator';
import { Aggregator, EmptyAggregator } from './aggregator';

export interface AggregatorProvider {

  getAggregator(): Aggregator

  updateAggregator(eventName: TestopithecusEvent): boolean

}

export class MapAggregatorProvider implements AggregatorProvider {

  private aggregators: Map<string, Aggregator> = new Map<string, Aggregator>();
  public currentAggregator: Nullable<Aggregator> = null

  constructor() {
    this.aggregators.set(EventNames.MODEL_SYNC_MESSAGE_LIST, new MessageListSyncAggregator())
  }

  public getAggregator(): Aggregator {
    const current: Nullable<Aggregator> = this.currentAggregator;
    if (current === null) {
      return new EmptyAggregator()
    } else {
      return current;
    }
  }

  public updateAggregator(event: TestopithecusEvent): boolean {
    const current: Nullable<Aggregator> = this.currentAggregator;
    if (current !== null && current.accepts(event)) {
      return false
    } else {
      this.currentAggregator = undefinedToNull(this.aggregators.get(event.name))
      return this.currentAggregator !== null
    }
  }

}

export class EmptyAggregatorProvider implements AggregatorProvider {

  public getAggregator(): Aggregator {
    return new EmptyAggregator();
  }

  public updateAggregator(eventName: TestopithecusEvent): boolean {
    return false;
  }

}
