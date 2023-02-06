import { MarkAsRead, MarkAsUnread } from '../../../mail/actions/markable-actions'
import {
  AccountSwitcherComponent,
  AllAccountSwitcherActions,
} from '../../../mail/components/account-switcher-component'
import { AllComposeActions, ComposeComponent } from '../../../mail/components/compose-component'
import {
  AllGroupOperationsActions,
  GroupOperationsComponent,
} from '../../../mail/components/group-operations-component'
import { AllLoginActions, LoginComponent } from '../../../mail/components/login-component'
import { AllMaillistActions, MaillistComponent } from '../../../mail/components/maillist-component'
import { AllMessageActions, MessageComponent } from '../../../mail/components/message-component'
import { UserAccount } from '../../../users/user-pool'
import { UserBehaviour } from './user-behaviour'

export function allActionsBehaviour(accounts: UserAccount[], multiLogin: boolean = false): UserBehaviour {
  return singleAccountBehaviour()
    .setActionProvider(LoginComponent.type, new AllLoginActions(accounts, multiLogin))
    .setActionProvider(AccountSwitcherComponent.type, new AllAccountSwitcherActions(accounts))
}

export function singleAccountBehaviour(): UserBehaviour {
  return new UserBehaviour()
    .setActionProvider(MaillistComponent.type, new AllMaillistActions())
    .setActionProvider(ComposeComponent.type, new AllComposeActions())
    .setActionProvider(GroupOperationsComponent.type, new AllGroupOperationsActions())
    .setActionProvider(MessageComponent.type, new AllMessageActions())
}

export function readUnreadUser(account: UserAccount): UserBehaviour {
  return allActionsBehaviour([account])
    .setActionProvider(LoginComponent.type, new AllLoginActions([account], false))
    .whitelistFor(MaillistComponent.type, MarkAsRead.type)
    .whitelistFor(MaillistComponent.type, MarkAsUnread.type)
}
