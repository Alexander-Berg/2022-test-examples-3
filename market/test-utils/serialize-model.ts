import {getMembers, IAnyStateTreeNode, IAnyType} from 'mobx-state-tree'

export default function serializeModel(model: IAnyStateTreeNode): {[K: string]: IAnyType} {
  const members = getMembers(model)
  const ownViews = members.views.filter((viewName) => !viewName.startsWith('$') && viewName !== 'toJSON')

  const viewsAndProperties = [...ownViews, ...Object.keys(members.properties)].reduce(
    (result, viewOrPropertyName) => ({
      ...result,
      [viewOrPropertyName]: model[viewOrPropertyName]
    }),
    {}
  )

  return viewsAndProperties
}
