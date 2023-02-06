import { Int32, Nullable } from '../../ys/ys'
import { LoginComponent } from '../mail/components/login-component'
import { ImapMailboxBuilder } from '../mail/mailbox-preparer'
import { FeatureID } from '../mbt/mbt-abstractions'
import { AbstractMBTTest, AccountType } from '../mbt/mbt-test'
import { StateMachine } from '../mbt/state-machine'
import { UserBehaviour } from '../mbt/walk/behaviour/user-behaviour'
import { MultiRunner } from '../mbt/walk/dfs-walk-strategy'
import { AppModelProvider, TestPlan } from '../mbt/walk/fixed-scenario-strategy'
import { ActionLimitsStrategy } from '../mbt/walk/limits/action-limits-strategy'
import { RandomActionChooser, UserBehaviourWalkStrategy } from '../mbt/walk/user-behaviour-walk-strategy'
import { UserAccount } from '../users/user-pool'
import { Logger } from '../utils/logger'
import { PseudoRandomProvider } from '../utils/pseudo-random'

export abstract class BaseUserBehaviourTest extends AbstractMBTTest {
  constructor(description: string,
              protected readonly pathLength: Int32,
              protected readonly logger: Logger,
              protected readonly seed: Int32) {
    super(description)
  }

  public abstract requiredAccounts(): AccountType[]

  public abstract prepareMailboxes(mailboxes: ImapMailboxBuilder[]): void

  public abstract getUserBehaviour(accounts: UserAccount[]): UserBehaviour

  public scenario(accounts: UserAccount[], modelProvider: Nullable<AppModelProvider>, supportedFeatures: FeatureID[]): TestPlan {
    if (modelProvider === null) {
      return TestPlan.empty()
    }
    const model = modelProvider!.takeAppModel()
    const random = new PseudoRandomProvider(this.seed)
    const behaviour = this.getUserBehaviour(accounts)
    const walkStrategy = new UserBehaviourWalkStrategy(behaviour, new RandomActionChooser(random), this.pathLength)
    // tslint:disable-next-line:prefer-const (swift requires var)
    let applicationModel = model.copy()
    applicationModel.supportedFeatures = supportedFeatures
    const stateMachine = new StateMachine(model, applicationModel, walkStrategy, this.logger)
    stateMachine.go(new LoginComponent())
    return TestPlan.empty().thenChain(walkStrategy.history)
  }
}

export abstract class FullCoverageBaseTest extends AbstractMBTTest {
  constructor(description: string, private logger: Logger) {
    super(description)
  }

  public abstract requiredAccounts(): AccountType[]

  public abstract prepareMailboxes(builders: ImapMailboxBuilder[]): void

  public abstract getUserBehaviour(userAccounts: UserAccount[]): UserBehaviour

  public abstract getActionLimits(): ActionLimitsStrategy

  public scenario(accounts: UserAccount[], modelProvider: Nullable<AppModelProvider>, supportedFeatures: FeatureID[]): TestPlan {
    if (modelProvider === null) {
      return TestPlan.empty()
    }
    const runner = new MultiRunner(new LoginComponent(), this.getUserBehaviour(accounts), this.getActionLimits(), modelProvider, supportedFeatures, this.logger)
    const path = runner.preparePath()
    const pathLength = path.length
    this.logger.log(`Optimal path length: ${pathLength}`)
    return TestPlan.empty().thenChain(path)
  }
}
