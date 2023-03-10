import { Int32, range } from '../../../ys/ys';
import { WYSIWIG } from '../mail-features';

export class WysiwygModel implements WYSIWIG {
  private symbols: string[] = []
  private styles: Array<Set<string>> = []

  public clearFormatting(from: Int32, to: Int32): void {
    for (const i of range(from, to)) {
      this.styles[i] = new Set<string>();
    }
  }

  public setStrong(from: Int32, to: Int32): void {
    for (const i of range(from, to)) {
      this.styles[i].add('strong')
    }
  }

  public setItalic(from: Int32, to: Int32): void {
    for (const i of range(from, to)) {
      this.styles[i].add('em')
    }
  }

  public appendText(index: number, text: string): void {
    const newBody: string[] = []
    const newStyles: Array<Set<string>> = []
    if (index < 1) {
      for (const i of range(0, text.length)) {
        const symbol = text.slice(i, i + 1)
        newBody.push(symbol)
        if (this.styles.length > index) {
          newStyles.push(this.styles[index])
        } else {
          newStyles.push(new Set<string>())
        }
      }
      for (const i of range(0, this.symbols.length)) {
        newBody.push(this.symbols[i])
        newStyles.push(this.styles[i])
      }
    } else {
      for (const i of range(0, this.symbols.length)) {
        if (i === index) {
          for (const j of range(0, text.length)) {
            const symbol = text.slice(j, j + 1)
            newBody.push(symbol)
            newStyles.push(this.styles[i - 1])
          }
        }
        newBody.push(this.symbols[i])
        newStyles.push(this.styles[i])
      }
    }
    this.symbols = newBody
    this.styles = newStyles
  }

  public getBody(): string {
    let newBody: string = ''
    let prevStyles: Set<string> = new Set<string>()
    let appendSymbol: string
    for (const i of range(0, this.symbols.length)) {
      appendSymbol = ''
      for (const style of this.styles[i].values()) {
        if (!prevStyles.has(style)) {
          appendSymbol += `<${style}>`
        }
      }
      for (const style of prevStyles.values()) {
        if (!this.styles[i].has(style)) {
          appendSymbol += `</${style}>`
        }
      }
      appendSymbol += this.symbols[i]
      newBody += appendSymbol
      prevStyles = this.styles[i]
    }
    return newBody;
  }

  public clear(): void {
    this.symbols = []
    this.styles = []
  }

  public getSymbols(): string[] {
    return this.symbols
  }

  public getStyles(): Array<Set<string>> {
    return this.styles
  }
}
