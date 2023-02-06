import { Int64, Nullable } from '../../ys/ys';

export class UserAccount {
  public constructor(public login: string, public password: string) {
  }
}

export class OAuthUserAccount {
  constructor(public account: UserAccount, public oauthToken: string) {
  }
}

export interface UserLock {
  lockedAccount(): UserAccount

  ping(newLockTtlMs: Int64): void

  release(): void
}

export interface UserPool {
  tryAcquire(tryAcquireTimeoutMs: Int64, lockTtlMs: Int64): Nullable<UserLock>

  // только для тестовых целей, чтобы сбросить все локи
  reset(): void
}
