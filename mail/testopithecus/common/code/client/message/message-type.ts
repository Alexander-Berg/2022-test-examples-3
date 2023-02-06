import { Int32, Nullable } from '../../../ys/ys'

// tslint:disable: no-bitwise
export type MessageTypeFlag = Int32
export class MessageTypeFlags {
  public static delivery: MessageTypeFlag = 1 << 0

  public static registration: MessageTypeFlag = 1 << 1

  public static social: MessageTypeFlag = 1 << 2

  public static people: MessageTypeFlag = 1 << 3

  public static eticket: MessageTypeFlag = 1 << 4

  public static eshop: MessageTypeFlag = 1 << 5

  public static notification: MessageTypeFlag = 1 << 6

  public static bounce: MessageTypeFlag = 1 << 7

  public static official: MessageTypeFlag = 1 << 8

  public static script: MessageTypeFlag = 1 << 9

  public static dating: MessageTypeFlag = 1 << 10

  public static greeting: MessageTypeFlag = 1 << 11

  public static news: MessageTypeFlag = 1 << 12

  public static sGrouponsite: MessageTypeFlag = 1 << 13

  public static sDatingsite: MessageTypeFlag = 1 << 14

  public static sETicket: MessageTypeFlag = 1 << 15

  public static sBank: MessageTypeFlag = 1 << 16

  public static sSocial: MessageTypeFlag = 1 << 17

  public static sTravel: MessageTypeFlag = 1 << 18

  public static sZDTicket: MessageTypeFlag = 1 << 19

  public static sRealty: MessageTypeFlag = 1 << 20

  public static sEShop: MessageTypeFlag = 1 << 21

  public static sCompany: MessageTypeFlag = 1 << 22

  public static sHotels: MessageTypeFlag = 1 << 23

  public static transact: MessageTypeFlag = 1 << 24

  public static personal: MessageTypeFlag = 1 << 25

  public static tNews: MessageTypeFlag = 1 << 26

  public static tSocial: MessageTypeFlag = 1 << 27

  public static tNotification: MessageTypeFlag = 1 << 28

  public static tPeople: MessageTypeFlag = 1 << 29
}

export function messageTypeMaskFromServerMessageTypes(types: readonly Int32[]): MessageTypeFlag {
  let result: MessageTypeFlag = 0
  for (const type of types) {
    const converted = serverMessageTypeFromInt32(type)
    if (converted !== null) {
      result = result | converted
    }
  }
  return result
}

function serverMessageTypeFromInt32(value: Int32): Nullable<MessageTypeFlag> {
  switch (value) {
    case 1: return MessageTypeFlags.delivery
    case 2: return MessageTypeFlags.registration
    case 3: return MessageTypeFlags.social
    case 4: return MessageTypeFlags.people
    case 5: return MessageTypeFlags.eticket
    case 6: return MessageTypeFlags.eshop
    case 7: return MessageTypeFlags.notification
    case 8: return MessageTypeFlags.bounce
    case 9: return MessageTypeFlags.official
    case 10: return MessageTypeFlags.script
    case 11: return MessageTypeFlags.dating
    case 12: return MessageTypeFlags.greeting
    case 13: return MessageTypeFlags.news
    case 14: return MessageTypeFlags.sGrouponsite
    case 15: return MessageTypeFlags.sDatingsite
    case 16: return MessageTypeFlags.sETicket
    case 17: return MessageTypeFlags.sBank
    case 18: return MessageTypeFlags.sSocial
    case 19: return MessageTypeFlags.sTravel
    case 20: return MessageTypeFlags.sZDTicket
    case 21: return MessageTypeFlags.sRealty
    case 23: return MessageTypeFlags.sEShop
    case 24: return MessageTypeFlags.sCompany
    case 35: return MessageTypeFlags.sHotels
    case 64: return MessageTypeFlags.transact
    case 65: return MessageTypeFlags.personal
    case 100: return MessageTypeFlags.tNews
    case 101: return MessageTypeFlags.tSocial
    case 102: return MessageTypeFlags.tNotification
    case 103: return MessageTypeFlags.tPeople
    default: return null
  }
}
