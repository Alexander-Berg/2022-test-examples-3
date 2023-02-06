import { Int32, Nullable } from '../../../ys/ys'
import { ArrayJSONItem, JSONItem, JSONItemKind, MapJSONItem, StringJSONItem } from '../../mail/logging/json-types'
import { NetworkStatus, NetworkStatusCode, networkStatusFromJSONItem } from '../status/network-status'

export class SettingsResponse {
  public constructor(
    public readonly status: NetworkStatus,
    public readonly payload: Nullable<StatusResponsePayload>,
  ) { }
}

export class StatusResponsePayload {
  public constructor(
    public readonly accountInformation: AccountInformation,
    public readonly userParameters: UserParameters,
    public readonly settingsSetup: SettingsSetup,
  ) { }
}

export function settingsResponseFromJSONItem(item: JSONItem): Nullable<SettingsResponse> {
  if (item.kind !== JSONItemKind.map) {
    return null
  }
  const map = item as MapJSONItem
  const status = networkStatusFromJSONItem(map.get('status')! as MapJSONItem)!
  if (status.code !== NetworkStatusCode.ok) {
    return new SettingsResponse(status, null)
  }
  const accountInformation =
    accountInformationFromJSONItem(map.getMap('account_information')!.get('account-information')! as MapJSONItem)
  const userParameters = userParametersFromJSONItem(map.getMap('get_user_parameters')!.get('body')! as MapJSONItem)
  const settingsSetup = settingsSetupFromJSONItem(map.getMap('settings_setup')!.get('body')! as MapJSONItem)
  return new SettingsResponse(status, new StatusResponsePayload(accountInformation, userParameters, settingsSetup))
}

export class Email {
  public constructor(
    public readonly login: string,
    public readonly domain: string,
  ) { }
}
export function stringToEmail(email: string): Nullable<Email> {
  const parts = email.split('@')
  if (parts.length !== 2) {
    return null
  }
  return new Email(parts[0], parts[1])
}

export class AccountInformation {
  public constructor(
    public readonly uid: string,
    public readonly suid: string,
    public readonly emails: readonly Email[],
    public readonly composeCheck: string,
  ) { }
}
function accountInformationFromJSONItem(item: MapJSONItem): AccountInformation {
  const uid = item.getString('uid')!
  const suid = item.getString('suid')!
  const composeCheck = item.getString('compose-check')!
  const emails: Email[] = []
  const emailsArray = item.getMap('emails')!.get('email')! as ArrayJSONItem
  for (const element of emailsArray.asArray()) {
    const mapElement = element as MapJSONItem
    const login = mapElement.getString('login')!
    const domain = mapElement.getString('domain')!
    emails.push(new Email(login, domain))
  }
  return new AccountInformation(uid, suid, emails, composeCheck)
}

export class UserParameters {
  public constructor(
    public readonly seasonsModifier: Nullable<string>,
    public readonly keyValues: Map<string, string>,
  ) { }
}
function userParametersFromJSONItem(item: MapJSONItem): UserParameters {
  const keyValues = new Map<string, string>();
  for (const key of item.asMap().keys()) {
    keyValues.set(key, item.getString(key)!)
  }
  return new UserParameters(item.getString('seasons-modifier'), keyValues)
}

export const enum SignaturePlace {
  none = 0,
  atEnd = 1,
  afterReply = 2,
}
export function signaturePlaceToInt32(value: SignaturePlace): Int32 {
  switch (value) {
    case SignaturePlace.none: return 0
    case SignaturePlace.atEnd: return 1
    case SignaturePlace.afterReply: return 2
  }
}
export function int32ToSignaturePlace(value: Int32): SignaturePlace {
  switch (value) {
    case 0: return SignaturePlace.none
    case 1: return SignaturePlace.atEnd
    case 2: return SignaturePlace.afterReply
    default: return SignaturePlace.none
  }
}

export class SettingsSetup {
  public constructor(
    public readonly colorScheme: string,
    public readonly fromName: string,
    public readonly defaultEmail: string,
    public readonly folderThreadView: boolean,
    public readonly quotationChar: string,
    public readonly mobileSign: string,
    public readonly signatureTop: SignaturePlace,
    public readonly replyTo: readonly string[],
  ) { }
}

function settingsSetupFromJSONItem(item: MapJSONItem): SettingsSetup {
  const colorScheme = item.getString('color_scheme')!
  const fromName = item.getString('from_name')!
  const defaultEmail = item.getString('default_email')!
  const folderThreadView = item.getString('folder_thread_view') === 'on'
  const mobileSignature = item.getString('mobile_sign')!
  const quotationChar = item.getString('quotation_char')!
  const signatureOnTop = item.hasKey('signature_top')
    ? (item.getString('signature_top') === 'on' ? SignaturePlace.afterReply : SignaturePlace.atEnd)
    : SignaturePlace.none
  const replyTo: string[] = []
  // TODO format is different
  // const replyToArray = item.getMap('reply_to')!.get('item')! as ArrayJSONItem
  // for (const array of replyToArray.asArray()) {
  //   if (array.kind === JSONItemKind.string) {
  //     replyTo.push((array as StringJSONItem).value)
  //   }
  // }
  return new SettingsSetup(
    colorScheme,
    fromName,
    defaultEmail,
    folderThreadView,
    quotationChar,
    mobileSignature,
    signatureOnTop,
    replyTo,
  )
}
