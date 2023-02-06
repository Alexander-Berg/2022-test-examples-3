import { App, MBTAction, MBTComponent } from '../../mbt/mbt-abstractions'
import { MBTComponentActions } from '../../mbt/walk/behaviour/user-behaviour'
import { UserAccount } from '../../users/user-pool'
import { YandexLoginAction } from '../actions/login-actions'
import { YandexLoginFeature } from '../mail-features'

export class LoginComponent implements MBTComponent {
  public static readonly type: string = 'LoginComponent'

  /**
   * Компонент экрана залогина.
   */
  public constructor() {
  }

  public getComponentType(): string {
    return LoginComponent.type
  }

  public assertMatches(model: App, application: App): void {
    // Кажется, мы не можем написать для этого компонента нормальные ассерты
  }

  public tostring(): string {
    return 'LoginComponent'
  }
}

export class AllLoginActions implements MBTComponentActions {
  public constructor(private accounts: UserAccount[], private multiLogin: boolean = false) {
  }

  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []

    YandexLoginFeature.get.performIfSupported(model, (mailboxModel) => {
      this.accounts.forEach((acc) => actions.push(new YandexLoginAction(acc)))
    })

    return actions
  }
}
