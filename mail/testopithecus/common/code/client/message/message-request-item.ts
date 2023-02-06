import { Int32, Nullable } from '../../../ys/ys'
import { ID, idToString, LabelID } from '../common/id'
import { MapJSONItem } from '../../mail/logging/json-types'
import { NetworkParams } from '../network/network-request'

export class MessageRequestItem {
  private constructor(
    public readonly fid: Nullable<ID>,
    private readonly tid: Nullable<ID>,
    private readonly lid: Nullable<LabelID>,
    private readonly first: Int32,
    private readonly last: Int32,
    private readonly threaded: boolean,
  ) {

  }
  public static threads(
    fid: ID,
    first: Int32,
    last: Int32,
  ): MessageRequestItem {
    return new MessageRequestItem(fid, null, null, first, last, true)
  }

  public static messagesInThread(
    tid: ID,
    first: Int32,
    last: Int32,
  ): MessageRequestItem {
    return new MessageRequestItem(null, tid, null, first, last, false)
  }

  public static messagesInFolder(
    fid: ID,
    first: Int32,
    last: Int32,
  ): MessageRequestItem {
    return new MessageRequestItem(fid, null, null, first, last, false)
  }

  public static messagesWithLabel(
    lid: LabelID,
    first: Int32,
    last: Int32,
  ): MessageRequestItem {
    return new MessageRequestItem(null, null, lid, first, last, false)
  }

  public params(): NetworkParams {
    const result = new MapJSONItem()
    if (this.fid !== null) {
      result.putString('fid', idToString(this.fid)!)
    }
    if (this.tid !== null) {
      result.putString('tid', idToString(this.tid)!)
    }
    if (this.lid !== null) {
      result.putString('lid', this.lid)
    }
    return result
      .putInt32('first', this.first)
      .putInt32('last', this.last)
      .putBoolean('threaded', this.threaded)
      .putString('md5', '')
      .putBoolean('returnIfModified', true)
  }
}
