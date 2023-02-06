import { Nullable } from '../../../ys/ys'
import { ArrayJSONItem, JSONItem, JSONItemKind, MapJSONItem } from '../../mail/logging/json-types'
import { FolderDTO, folderFromJSONItem } from '../folder/folderDTO'
import { Label, labelFromJSONItem } from '../label/label'
import { NetworkStatusCode } from '../status/network-status'
import { ContainerStats, containerStatsFromJSONItem } from './container-stats'

export class Container {
  public constructor(
    public readonly stats: ContainerStats,
    public readonly folders: readonly FolderDTO[],
    public readonly labels: readonly Label[],
  ) { }
}

export function containerFromJSONItem(item: JSONItem): Nullable<Container> {
  if (item.kind !== JSONItemKind.array) {
    return null
  }
  const array = item as ArrayJSONItem
  const containerStats = containerStatsFromJSONItem(array.get(0))
  if (containerStats === null) {
    return null
  }

  const folders: FolderDTO[] = []
  const labels: Label[] = []
  if (containerStats.status.code === NetworkStatusCode.ok) {
    for (const value of array.asArray()) {
      const map = value as MapJSONItem
      if (map.hasKey('fid')) {
        folders.push(folderFromJSONItem(map)!)
      } else if (map.hasKey('lid')) {
        labels.push(labelFromJSONItem(map)!)
      }
    }
  }

  return new Container(containerStats, folders, labels)
}
