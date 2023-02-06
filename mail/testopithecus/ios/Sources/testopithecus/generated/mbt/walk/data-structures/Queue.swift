// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/walk/data-structures/queue.ts >>>

import Foundation

open class Queue<T> {
  public var q1: YSArray<T> = YSArray()
  public var q2: YSArray<T> = YSArray()
  open func push(_ item: T) {
    q1.push(item)
  }

  open func pop() {
    move()
    q2.pop()
  }

  open func clear() {
    q1 = YSArray()
    q2 = YSArray()
  }

  @discardableResult
  open func size() -> Int32 {
    return q1.length + q2.length
  }

  @discardableResult
  open func front() -> T {
    move()
    return q2[self.q2.length - 1]
  }

  private func move() {
    if q2.length > 0 {
      return
    }
    while q1.length > 0 {
      let element = q1.pop()
      q2.push(element!)
    }
  }
}