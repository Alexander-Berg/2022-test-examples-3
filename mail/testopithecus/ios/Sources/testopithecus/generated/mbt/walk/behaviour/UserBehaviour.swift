// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/walk/behaviour/user-behaviour.ts >>>

import Foundation

open class UserBehaviour {
  private var actionProviders: YSMap<MBTComponentType, MBTComponentActions> = YSMap<MBTComponentType, MBTComponentActions>()
  public init() {}

  @discardableResult
  open func setActionProvider(_ type: MBTComponentType, _ actionProvider: MBTComponentActions) -> UserBehaviour {
    actionProviders.set(type, actionProvider)
    return self
  }

  @discardableResult
  open func setActions(_ type: MBTComponentType, _ actions: YSArray<MBTAction>) -> UserBehaviour {
    setActionProvider(type, ListActions(actions))
    return self
  }

  @discardableResult
  open func blacklist(_ action: MBTActionType) -> UserBehaviour {
    for componentType in actionProviders.keys() {
      let typeActions = actionProviders.get(componentType)!
      actionProviders.set(componentType, BlacklistActions(YSArray(action), typeActions))
    }
    return self
  }

  @discardableResult
  open func whitelist(_ action: MBTActionType) -> UserBehaviour {
    for componentType in actionProviders.keys() {
      let typeActions = actionProviders.get(componentType)!
      actionProviders.set(componentType, WhitelistActions(YSArray(action), typeActions))
    }
    return self
  }

  @discardableResult
  open func whitelistFor(_ type: MBTComponentType, _ action: MBTActionType) -> UserBehaviour {
    actionProviders.set(type, WhitelistActions(YSArray(action), actionProviders.get(type)!))
    return self
  }

  @discardableResult
  open func single(_ type: MBTComponentType, _ action: MBTAction) -> UserBehaviour {
    return setActionProvider(type, ListActions.single(action))
  }

  @discardableResult
  open func getActions(_ model: App, _ component: MBTComponent) -> YSArray<MBTAction> {
    let actionsProvider: MBTComponentActions! = undefinedToNull(actionProviders.get(component.getComponentType()))
    if actionsProvider == nil {
      return YSArray()
    }
    return actionsProvider!.getActions(model)
  }
}

public protocol MBTComponentActions {
  @discardableResult
  func getActions(_ model: App) -> YSArray<MBTAction>
}

open class ListActions: MBTComponentActions {
  private var actions: YSArray<MBTAction>
  public init(_ actions: YSArray<MBTAction>) {
    self.actions = actions
  }

  @discardableResult
  open class func single(_ action: MBTAction) -> ListActions {
    return ListActions(YSArray(action))
  }

  @discardableResult
  open class func empty() -> ListActions {
    return ListActions(YSArray())
  }

  @discardableResult
  open func getActions(_: App) -> YSArray<MBTAction> {
    return actions
  }
}

open class BlacklistActions: MBTComponentActions {
  private var blacklistActions: YSArray<MBTActionType>
  private var delegate: MBTComponentActions
  public init(_ blacklistActions: YSArray<MBTActionType>, _ delegate: MBTComponentActions) {
    self.blacklistActions = blacklistActions
    self.delegate = delegate
  }

  @discardableResult
  open func getActions(_ model: App) -> YSArray<MBTAction> {
    return delegate.getActions(model).filter {
      action in
      !self.blacklistActions.includes(action.getActionType())
    }
  }
}

open class WhitelistActions: MBTComponentActions {
  private var whitelistActions: YSArray<MBTActionType>
  private var delegate: MBTComponentActions
  public init(_ whitelistActions: YSArray<MBTActionType>, _ delegate: MBTComponentActions) {
    self.whitelistActions = whitelistActions
    self.delegate = delegate
  }

  @discardableResult
  open func getActions(_ model: App) -> YSArray<MBTAction> {
    let actions = delegate.getActions(model)
    let whitelistActions = actions.filter {
      a in
      self.whitelistActions.includes(a.getActionType())
    }
    if whitelistActions.length == 0 {
      return actions
    }
    return whitelistActions
  }
}