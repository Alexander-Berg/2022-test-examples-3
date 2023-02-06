import { Int32, int64, Int64, Nullable } from '../../../ys/ys'
import { MailboxModel } from '../../mail/model/mail-model'
import { Logger } from '../../utils/logger'
import { FeatureID, MBTAction, MBTComponent } from '../mbt-abstractions'
import { StateMachine} from '../state-machine'
import { UserBehaviour } from './behaviour/user-behaviour'
import { Graph } from './data-structures/graph'
import { Stack } from './data-structures/stack'
import { AppModelProvider } from './fixed-scenario-strategy'
import { LongestPathAlgo } from './graph-algorithms/longest-path-algo'
import { FullHashStrategy } from './hash/full-hash-strategy'
import { HashStrategy } from './hash/hash-builder'
import { ActionLimitsStrategy } from './limits/action-limits-strategy'
import { ActionChooser, UserBehaviourWalkStrategy } from './user-behaviour-walk-strategy'

export class DfsWalkStrategy implements ActionChooser {
  public graph: Graph<MBTAction> = new Graph()
  public stack: Stack<Int32> = new Stack<Int32>()
  public stateStack: Stack<MailboxModel> = new Stack<MailboxModel>()
  public componentStack: Stack<MBTComponent> = new Stack<MBTComponent>()
  public hashStack: Stack<Int64> = new Stack<Int64>()

  public previousVertex: Int64 = int64(-1)
  private used: Set<Int64> = new Set<Int64>()
  private actionStack: Stack<MBTAction> = new Stack<MBTAction>()
  private numberOfEdge: Int32 = -1

  constructor(private hashProvider: HashProvider, private actionLimits: ActionLimitsStrategy) {
  }

  public choose(possibleActions: MBTAction[], component: MBTComponent): Nullable<MBTAction> {
    const currentHash: Int64 = this.hashProvider.getHash()
    this.stateStack.push(this.hashProvider.getModelCopy()!)
    this.hashStack.push(currentHash)
    this.componentStack.push(component)
    if (this.numberOfEdge >= 0) {
      this.graph.addEdgeVV(this.previousVertex, currentHash, this.actionStack.top())
    }
    if (this.used.has(currentHash)) {
      this.stepBack()
      return null
    }
    this.used.add(currentHash)
    this.graph.addVertex(currentHash)

    const actionIndex = this.graph.getDegreeV(currentHash)
    if (possibleActions.length <= actionIndex || !this.actionLimits.check(this.actionStack)) {
      this.stepBack()
      return null
    }

    this.previousVertex = currentHash
    this.numberOfEdge = actionIndex
    this.stack.push(this.numberOfEdge)
    this.actionStack.push(possibleActions[this.numberOfEdge])
    return possibleActions[this.numberOfEdge]
  }

  private stepBack(): void {
    this.stateStack.pop()
    this.hashStack.pop()
    this.componentStack.pop()
    this.stack.pop()
    this.actionStack.pop()
    this.used.delete(this.previousVertex)
    this.numberOfEdge = -1
  }
}

export class MultiRunner {

  public readonly walkStrategyWithState: DfsWalkStrategy
  private readonly hashProvider: HashProvider
  /**
   * Множественная запускалка для обхода всего графа
   *
   * @param component - тестируемая компонента
   * @param behaviour - куда можно ходить
   * @param actionLimits - ограничениея на количество действий
   * @param modelProvider - скачивалска начального состояния модели
   * @param supportedFeatures - поддерживаемые приложением фичи
   * @param logger - логгер
   */
  constructor(private component: MBTComponent,
              private behaviour: UserBehaviour,
              private actionLimits: ActionLimitsStrategy,
              private modelProvider: AppModelProvider,
              private supportedFeatures: FeatureID[],
              private logger: Logger) {
    const hashStrategy = new FullHashStrategy()
    this.hashProvider = new HashProvider(hashStrategy)
    this.walkStrategyWithState = new DfsWalkStrategy(this.hashProvider, actionLimits)
  }

  public preparePath(): MBTAction[] {
    let model = this.modelProvider.takeAppModel()
    // mimic under application
    model.supportedFeatures = this.supportedFeatures

    let model2 = model.copy()
    let model3 = model.copy()

    this.logger.log('DFS started')
    while (true) {
      this.hashProvider.setModel(model2 as MailboxModel)
      const modelVsModel = new StateMachine(model2, model3, new UserBehaviourWalkStrategy(this.behaviour, this.walkStrategyWithState), this.logger)
      modelVsModel.go(this.component)

      if (this.walkStrategyWithState.stateStack.size() === 0) {
        break
      }
      this.component = this.walkStrategyWithState.componentStack.top()
      model2 = this.walkStrategyWithState.stateStack.top().copy()
      model3 = this.walkStrategyWithState.stateStack.top().copy()
      this.walkStrategyWithState.stateStack.pop()
      this.walkStrategyWithState.hashStack.pop()
      this.walkStrategyWithState.componentStack.pop()
      if (this.walkStrategyWithState.hashStack.size() > 0) {
        this.walkStrategyWithState.previousVertex = this.walkStrategyWithState.hashStack.top()
      }
    }
    this.logger.log('DFS finished\n')
    this.logger.log(`Count of vertexes = ${this.walkStrategyWithState.graph.size()}`)
    this.logger.log(`Count of edges = ${this.walkStrategyWithState.graph.countOfEdges()}`)

    // walkStrategyWithState.graph.print('graph.txt');

    return LongestPathAlgo.getLongestPath(this.walkStrategyWithState.graph, this.logger)
  }
}

export class HashProvider {
  private model: Nullable<MailboxModel> = null

  constructor(private hashStrategy: HashStrategy) {
  }

  public setModel(model: MailboxModel): void {
    this.model = model
  }

  public getHash(): Int64 {
    if (this.model !== null) {
      return this.hashStrategy.getMailboxModelHash(this.model!)
    }
    return int64(-1)
  }

  public getModelCopy(): Nullable<MailboxModel> {
    const modelSnapshot = this.model
    if (modelSnapshot !== null) {
      const mailboxModel: MailboxModel = modelSnapshot.copy() as MailboxModel
      return mailboxModel
    }
    return null
  }
}
