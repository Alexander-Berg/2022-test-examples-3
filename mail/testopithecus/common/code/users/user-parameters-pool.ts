import { int64, Int64, Nullable, range, stringToInt64, undefinedToNull } from '../../ys/ys';
import { MailboxClient } from '../client/mailbox-client';
import { currentTimeMs } from '../mail/logging/logging-utils';
import { Logger } from '../utils/logger';
import { UserLock, UserAccount, UserPool } from './user-pool'

export class UserParametersUserPool implements UserPool {
  public static EXPIRE_AT_PARAM: string = 'mobile_lock_until'

  constructor(private client: MailboxClient, private account: UserAccount, private logger: Logger) {
  }

  public tryAcquire(tryAcquireTimeoutMs: Int64, lockTtlMs: Int64): Nullable<UserLock> {
    this.logger.log(`Will try to acquire lock for ${tryAcquireTimeoutMs}ms with TTL ${lockTtlMs}ms`)
    const start = currentTimeMs()
    while (currentTimeMs() < start + tryAcquireTimeoutMs) {
      const now = currentTimeMs()
      const expirationTime = this.getExpirationTime()
      if (expirationTime === null || expirationTime < now) {
        const expireAt = now + lockTtlMs
        this.logger.log(`Locking until ${expireAt}`)
        this.client.setParameter(UserParametersUserPool.EXPIRE_AT_PARAM, `${expireAt}`)
        if (this.isLocked(expireAt)) {
          this.logger.log('Lock acquired')
          return new UserParametersLock(this.client, this.account, this.logger)
        }
      } else {
        this.logger.log(`${(expirationTime! - now) / int64(1000)} seconds before unlock`)
      }
    }
    this.logger.log('Can\'t acquire lock')
    return null;
  }

  public reset(): void {
    new UserParametersLock(this.client, this.account, this.logger).release()
  }

  private getExpirationTime(): Nullable<Int64> {
    const userParameters = this.client.getSettings().payload!.userParameters.keyValues
    const expireAt = undefinedToNull(userParameters.get(UserParametersUserPool.EXPIRE_AT_PARAM))
    if (expireAt === null || expireAt === '') {
      return null
    }
    return stringToInt64(expireAt)!
  }

  private isLocked(expireAt: Int64): boolean {
    for (const _ of range(0, 3)) {
      if (this.getExpirationTime() !== expireAt) {
        return false
      }
    }
    return true
  }
}

export class UserParametersLock implements UserLock {
  constructor(private client: MailboxClient, private account: UserAccount, private logger: Logger) {
  }

  public lockedAccount(): UserAccount {
    return this.account;
  }

  public ping(newTtlMs: Int64): void {
    this.logger.log('Pinging lock')
    const expireAt = currentTimeMs() + newTtlMs
    this.client.setParameter(UserParametersUserPool.EXPIRE_AT_PARAM, `${expireAt}`)
  }

  public release(): void {
    this.client.setParameter(UserParametersUserPool.EXPIRE_AT_PARAM, '')
    this.logger.log('Lock released')
  }
}
