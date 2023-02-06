import { TestopithecusEvent } from '../mail/logging/testopithecus-event';
import {
  App, Feature,
  FeatureID,
  MBTAction, MBTActionType,
  MBTComponent, MBTHistory,
} from './mbt-abstractions';

export abstract class BaseSimpleAction<F, C> implements MBTAction {
  constructor(private type: MBTActionType) {
  }

  public abstract requiredFeature(): Feature<F>

  public supported(modelFeatures: FeatureID[], applicationFearures: FeatureID[]): boolean {
    return this.requiredFeature().includedAll(modelFeatures, applicationFearures)
  }

  public canBePerformed(model: App): boolean {
    const featuredModel = this.requiredFeature().forceCast(model)
    return this.canBePerformedImpl(featuredModel)
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    const currentComponent = history.currentComponent;
    const modelFeature: F = this.requiredFeature().forceCast(model)
    const applicationFeature: F = this.requiredFeature().forceCast(application)
    // @ts-ignore
    const component = currentComponent as C
    this.performImpl(modelFeature, component);
    return this.performImpl(applicationFeature, component);
  }

  public abstract events(): TestopithecusEvent[]

  public canBePerformedImpl(model: F): boolean {
    return true
  }

  public abstract performImpl(modelOrApplication: F, currentComponent: C): MBTComponent // TODO: M & App

  public getActionType(): MBTActionType {
    return this.type
  }

  public tostring(): string {
    return this.getActionType()
  }
}
