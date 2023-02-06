import { int64, Int64, int64ToInt32, Nullable } from '../../ys/ys';
import { currentTimeMs } from '../mail/logging/logging-utils';
import { UserLock, UserAccount, UserPool } from './user-pool';
import { UserServiceAccount, UserService } from './user-service';

export class UserServicePool implements UserPool {
  constructor(private userService: UserService, private tag: Nullable<string>) {
  }

  public tryAcquire(tryAcquireTimeoutMs: Int64, lockTtlMs: Int64): Nullable<UserLock> {
    const start = currentTimeMs()
    while (currentTimeMs() < start + tryAcquireTimeoutMs) {
      const user = this.userService.getAccount(this.tag, int64ToInt32(lockTtlMs / int64(1000)), false, null)
      if (user !== null) {
        return new UserServiceLock(this.userService, user!)
      }
    }
    return null
  }

  public reset(): void {
    const account = this.userService.getAccount(this.tag, 0, true, null)
    if (account === null) {
      return
    }
    this.userService.unlockAccount(account!.uid)
  }
}

export class UserServiceLock implements UserLock {
  constructor(private userService: UserService, private account: UserServiceAccount) {
  }

  public lockedAccount(): UserAccount {
    return new UserAccount(this.account.login, this.account.password)
  }

  public ping(newLockTtlMs: Int64): void {
    this.userService.getAccount(null, int64ToInt32(newLockTtlMs / int64(1000)), true, this.account.uid)
  }

  public release(): void {
    this.userService.unlockAccount(this.account.uid)
  }
}
