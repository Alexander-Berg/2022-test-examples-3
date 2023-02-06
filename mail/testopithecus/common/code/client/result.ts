import { Nullable } from '../../ys/ys'
import { Failure } from './failure'

export function getVoid(): void {
  // Empty by design. For typematching like:
  // return new Result<void>(Void(), null)
}

export class Result<T> {
  public constructor(private readonly value: Nullable<T>, private readonly error: Nullable<Failure>) { }

  public isValue(): boolean {
    return this.error === null
  }

  public isError(): boolean {
    return this.error !== null
  }

  public getValue(): T {
    return this.value!
  }

  public getError(): Failure {
    return this.error!
  }

  public withValue<U>(f: (value: T) => U): Nullable<U> {
    if (this.isValue()) {
      return f(this.getValue())
    }
    return null
  }
}
