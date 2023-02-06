export enum RequestMode {
  PROXY = 'proxy',
  PLAY = 'play',
  SAVE = 'save',
  CREATE = 'create'
}

export function getRequestsMode(): RequestMode {
  return hermione.ctx.clementMode as RequestMode
}

export function isPlayMode() {
  return getRequestsMode() === RequestMode.PLAY
}

export function isSaveMode() {
  return getRequestsMode() === RequestMode.SAVE
}
