// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/wysiwyg-model.ts >>>

import Foundation

open class WysiwygModel: WYSIWIG {
  private var symbols: YSArray<String> = YSArray()
  private var styles: YSArray<YSSet<String>> = YSArray()
  open func clearFormatting(_ from: Int32, _ to: Int32) {
    for i in stride(from: from, to: to, by: 1) {
      styles[i] = YSSet<String>()
    }
  }

  open func setStrong(_ from: Int32, _ to: Int32) {
    for i in stride(from: from, to: to, by: 1) {
      styles[i].add("strong")
    }
  }

  open func setItalic(_ from: Int32, _ to: Int32) {
    for i in stride(from: from, to: to, by: 1) {
      styles[i].add("em")
    }
  }

  open func appendText(_ index: Int32, _ text: String) {
    let newBody: YSArray<String> = YSArray()
    let newStyles: YSArray<YSSet<String>> = YSArray()
    if index < 1 {
      for i in stride(from: 0, to: text.length, by: 1) {
        let symbol = text.slice(i, i + 1)
        newBody.push(symbol)
        if styles.length > index {
          newStyles.push(styles[index])
        } else {
          newStyles.push(YSSet<String>())
        }
      }
      for i in stride(from: 0, to: symbols.length, by: 1) {
        newBody.push(symbols[i])
        newStyles.push(styles[i])
      }
    } else {
      for i in stride(from: 0, to: symbols.length, by: 1) {
        if i == index {
          for j in stride(from: 0, to: text.length, by: 1) {
            let symbol = text.slice(j, j + 1)
            newBody.push(symbol)
            newStyles.push(styles[i - 1])
          }
        }
        newBody.push(symbols[i])
        newStyles.push(styles[i])
      }
    }
    symbols = newBody
    styles = newStyles
  }

  @discardableResult
  open func getBody() -> String {
    var newBody: String = ""
    var prevStyles: YSSet<String> = YSSet<String>()
    var appendSymbol: String
    for i in stride(from: 0, to: symbols.length, by: 1) {
      appendSymbol = ""
      for style in styles[i].values() {
        if !prevStyles.has(style) {
          appendSymbol += "<\(style)>"
        }
      }
      for style in prevStyles.values() {
        if !styles[i].has(style) {
          appendSymbol += "</\(style)>"
        }
      }
      appendSymbol += symbols[i]
      newBody += appendSymbol
      prevStyles = styles[i]
    }
    return newBody
  }

  open func clear() {
    symbols = YSArray()
    styles = YSArray()
  }

  @discardableResult
  open func getSymbols() -> YSArray<String> {
    return symbols
  }

  @discardableResult
  open func getStyles() -> YSArray<YSSet<String>> {
    return styles
  }
}