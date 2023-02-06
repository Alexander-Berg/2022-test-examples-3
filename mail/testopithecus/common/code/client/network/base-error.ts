import { Nullable } from '../../../ys/ys'
import { Failure } from '../failure'

export interface BaseError extends Failure {
  readonly inner: Nullable<Failure>
}
