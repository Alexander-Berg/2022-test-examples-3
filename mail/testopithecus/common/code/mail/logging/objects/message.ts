import { Int64 } from '../../../../ys/ys';
import { MapJSONItem } from '../json-types'

export class MessageDTO {

  constructor(
    public mid: Int64,
    public timestamp: Int64,
  ) { }

  public toMap(): Map<string, any> {
    const map = new Map<string, any>();
    map.set('mid', this.mid);
    map.set('timestamp', this.timestamp);
    return map;
  }

  public toJson(): MapJSONItem {
    return new MapJSONItem()
      .putInt64('mid', this.mid)
      .putInt64('timestamp', this.timestamp)
  }

  public static fromMap(map: Map<string, any>): MessageDTO {
    return new MessageDTO(
      map.get('mid') as Int64,
      map.get('timestamp') as Int64,
    )
  }
}
