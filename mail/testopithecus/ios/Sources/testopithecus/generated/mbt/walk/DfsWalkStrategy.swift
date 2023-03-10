// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/walk/dfs-walk-strategy.ts >>>

import Foundation

open class DfsWalkStrategy: ActionChooser {
  public var graph: Graph<MBTAction> = Graph()
  public var stack: Stack<Int32> = Stack<Int32>()
  public var stateStack: Stack<MailboxModel> = Stack<MailboxModel>()
  public var componentStack: Stack<MBTComponent> = Stack<MBTComponent>()
  public var hashStack: Stack<Int64> = Stack<Int64>()
  public var previousVertex: Int64 = int64(-1)
  private var used: YSSet<Int64> = YSSet<Int64>()
  private var actionStack: Stack<MBTAction> = Stack<MBTAction>()
  private var numberOfEdge: Int32 = -1
  private var hashProvider: HashProvider
  private var actionLimits: ActionLimitsStrategy
  public init(_ hashProvider: HashProvider, _ actionLimits: ActionLimitsStrategy) {
    self.hashProvider = hashProvider
    self.actionLimits = actionLimits
  }

  @discardableResult
  open func choose(_ possibleActions: YSArray<MBTAction>, _ component: MBTComponent) -> MBTAction! {
    let currentHash: Int64 = hashProvider.getHash()
    stateStack.push(hashProvider.getModelCopy()!)
    hashStack.push(currentHash)
    componentStack.push(component)
    if numberOfEdge >= 0 {
      graph.addEdgeVV(previousVertex, currentHash, actionStack.top())
    }
    if used.has(currentHash) {
      stepBack()
      return nil
    }
    used.add(currentHash)
    graph.addVertex(currentHash)
    let actionIndex = graph.getDegreeV(currentHash)
    if possibleActions.length <= actionIndex || !actionLimits.check(actionStack) {
      stepBack()
      return nil
    }
    previousVertex = currentHash
    numberOfEdge = actionIndex
    stack.push(numberOfEdge)
    actionStack.push(possibleActions[self.numberOfEdge])
    return possibleActions[self.numberOfEdge]
  }

  private func stepBack() {
    stateStack.pop()
    hashStack.pop()
    componentStack.pop()
    stack.pop()
    actionStack.pop()
    used.delete(previousVertex)
    numberOfEdge = -1
  }
}

open class MultiRunner {
  public let walkStrategyWithState: DfsWalkStrategy
  private let hashProvider: HashProvider
  private var component: MBTComponent
  private var behaviour: UserBehaviour
  private var actionLimits: ActionLimitsStrategy
  private var modelProvider: AppModelProvider
  private var supportedFeatures: YSArray<FeatureID>
  private var logger: Logger
  public init(_ component: MBTComponent, _ behaviour: UserBehaviour, _ actionLimits: ActionLimitsStrategy, _ modelProvider: AppModelProvider, _ supportedFeatures: YSArray<FeatureID>, _ logger: Logger) {
    self.component = component
    self.behaviour = behaviour
    self.actionLimits = actionLimits
    self.modelProvider = modelProvider
    self.supportedFeatures = supportedFeatures
    self.logger = logger
    let hashStrategy = FullHashStrategy()
    hashProvider = HashProvider(hashStrategy)
    walkStrategyWithState = DfsWalkStrategy(hashProvider, actionLimits)
  }

  @discardableResult
  open func preparePath() -> YSArray<MBTAction> {
    var model = modelProvider.takeAppModel()
    model.supportedFeatures = supportedFeatures
    var model2 = model.copy()
    var model3 = model.copy()
    logger.log("DFS started")
    while true {
      hashProvider.setModel(model2 as! MailboxModel)
      let modelVsModel = StateMachine(model2, model3, UserBehaviourWalkStrategy(behaviour, walkStrategyWithState), logger)
      modelVsModel.go(component)
      if walkStrategyWithState.stateStack.size() == 0 {
        break
      }
      component = walkStrategyWithState.componentStack.top()
      model2 = walkStrategyWithState.stateStack.top().copy()
      model3 = walkStrategyWithState.stateStack.top().copy()
      walkStrategyWithState.stateStack.pop()
      walkStrategyWithState.hashStack.pop()
      walkStrategyWithState.componentStack.pop()
      if walkStrategyWithState.hashStack.size() > 0 {
        walkStrategyWithState.previousVertex = walkStrategyWithState.hashStack.top()
      }
    }
    logger.log("DFS finished\n")
    logger.log("Count of vertexes = \(walkStrategyWithState.graph.size())")
    logger.log("Count of edges = \(walkStrategyWithState.graph.countOfEdges())")
    return LongestPathAlgo.getLongestPath(walkStrategyWithState.graph, logger)
  }
}

open class HashProvider {
  private var model: MailboxModel!
  private var hashStrategy: HashStrategy
  public init(_ hashStrategy: HashStrategy) {
    self.hashStrategy = hashStrategy
  }

  open func setModel(_ model: MailboxModel) {
    self.model = model
  }

  @discardableResult
  open func getHash() -> Int64 {
    if model != nil {
      return hashStrategy.getMailboxModelHash(model!)
    }
    return int64(-1)
  }

  @discardableResult
  open func getModelCopy() -> MailboxModel! {
    let modelSnapshot: MailboxModel! = model
    if modelSnapshot != nil {
      let mailboxModel: MailboxModel = modelSnapshot.copy() as! MailboxModel
      return mailboxModel
    }
    return nil
  }
}
