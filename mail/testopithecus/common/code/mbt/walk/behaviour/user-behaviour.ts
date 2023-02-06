import { undefinedToNull } from '../../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTComponentType } from '../../mbt-abstractions'

export class UserBehaviour {
  private actionProviders: Map<MBTComponentType, MBTComponentActions> = new Map<MBTComponentType, MBTComponentActions>()

  public constructor() {
  }

  public setActionProvider(type: MBTComponentType, actionProvider: MBTComponentActions): UserBehaviour {
    this.actionProviders.set(type, actionProvider)
    return this
  }

  public setActions(type: MBTComponentType, actions: MBTAction[]): UserBehaviour {
    this.setActionProvider(type, new ListActions(actions))
    return this
  }

  public blacklist(action: MBTActionType): UserBehaviour {
    for (const componentType of this.actionProviders.keys()) {
      const typeActions = this.actionProviders.get(componentType)!
      this.actionProviders.set(componentType, new BlacklistActions([action], typeActions))
    }
    return this
  }

  public whitelist(action: MBTActionType): UserBehaviour {
    for (const componentType of this.actionProviders.keys()) {
      const typeActions = this.actionProviders.get(componentType)!
      this.actionProviders.set(componentType, new WhitelistActions([action], typeActions))
    }
    return this
  }

  public whitelistFor(type: MBTComponentType, action: MBTActionType): UserBehaviour {
    this.actionProviders.set(type, new WhitelistActions([action], this.actionProviders.get(type)!))
    return this
  }

  public single(type: MBTComponentType, action: MBTAction): UserBehaviour {
    return this.setActionProvider(type, ListActions.single(action))
  }

  public getActions(model: App, component: MBTComponent): MBTAction[] {
    const actionsProvider = undefinedToNull(this.actionProviders.get(component.getComponentType()))
    if (actionsProvider === null) {
      return []
    }
    return actionsProvider!.getActions(model)
  }

}

export interface MBTComponentActions {
  getActions(model: App): MBTAction[]
}

export class ListActions implements MBTComponentActions {
  constructor(private actions: MBTAction[]) {
  }

  public static single(action: MBTAction): ListActions {
    return new ListActions([action])
  }

  public static empty(): ListActions {
    return new ListActions([])
  }

  public getActions(model: App): MBTAction[] {
    return this.actions
  }
}

export class BlacklistActions implements MBTComponentActions {
  constructor(private blacklistActions: MBTActionType[], private delegate: MBTComponentActions) {
  }

  public getActions(model: App): MBTAction[] {
    return this.delegate.getActions(model)
      .filter((action) => !this.blacklistActions.includes(action.getActionType()))
  }
}

export class WhitelistActions implements MBTComponentActions {
  constructor(private whitelistActions: MBTActionType[], private delegate: MBTComponentActions) {
  }

  public getActions(model: App): MBTAction[] {
    const actions = this.delegate.getActions(model)
    const whitelistActions = actions.filter((a) => this.whitelistActions.includes(a.getActionType()))
    if (whitelistActions.length === 0) {
      return actions
    }
    return whitelistActions
  }
}
