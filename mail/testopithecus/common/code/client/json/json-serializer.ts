import { Decodable, Encodable } from '../../../ys/ys'
import { JSONItem } from '../../mail/logging/json-types'
import { RuntimeClassInfo } from '../../utils/runtime-info'
import { Result } from '../result'

export interface JSONSerializer {
  serialize(item: JSONItem): Result<string>
  deserialize<T>(item: string, materializer: (json: JSONItem) => Result<T>): Result<T>

  serializeEncodable<T extends Encodable>(item: T): Result<string>
  deserializeDecodable<T extends Decodable>(runtimeClassInfo: RuntimeClassInfo, item: string): Result<T>
}
