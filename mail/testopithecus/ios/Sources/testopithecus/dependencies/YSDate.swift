import Foundation

public final class YSDate {
  private static let formatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    formatter.locale = Locale(identifier: "en_US")
    return formatter
  }()

  internal let value: Date

  private init(_ value: Date) {
    self.value = value
  }

  public convenience init(_ string: String) {
    self.init(YSDate.formatter.date(from: string)!)
  }

  public convenience init(_ int: Int64) {
    self.init(Date(timeIntervalSince1970: TimeInterval(int) / 100.0))
  }

  public convenience init() {
    self.init(Date())
  }

  public static func now() -> Int64 {
    return convert(date: Date())
  }

  public static func convert(date: Date) -> Int64 {
    return Int64(date.timeIntervalSince1970 * 100)
  }
  
  public func getMonth() -> Int32 {
    return numericCast(Calendar.current.dateComponents([.month], from: Date()).month!)
  }
}

extension YSDate: CustomStringConvertible, CustomDebugStringConvertible {
  public var description: String {
    return self.value.description
  }
  public var debugDescription: String {
    return self.value.debugDescription
  }
}
