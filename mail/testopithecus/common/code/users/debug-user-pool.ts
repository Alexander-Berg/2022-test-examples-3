import { Int64, Nullable } from '../../ys/ys';
import { UserLock, UserAccount, UserPool } from './user-pool';

export class DebugUserPool implements UserPool {
  constructor(private account: UserAccount) {
  }

  public tryAcquire(tryAcquireTimeoutMs: Int64, lockTtlMs: Int64): Nullable<UserLock> {
    return new DebugLock(this.account)
  }

  public reset(): void {
  }
}

export class DebugLock implements UserLock {
  constructor(private account: UserAccount) {
  }

  public lockedAccount(): UserAccount {
    return this.account
  }

  public ping(newLockTtlMs: bigint): void {
  }

  public release(): void {
  }
}
