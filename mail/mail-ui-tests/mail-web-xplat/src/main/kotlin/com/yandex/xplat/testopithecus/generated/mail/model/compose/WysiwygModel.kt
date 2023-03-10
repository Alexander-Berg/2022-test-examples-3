// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/compose/wysiwyg-model.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.YSSet
import com.yandex.xplat.common.slice

public open class WysiwygModel: WYSIWIG {
    private var symbols: YSArray<String> = mutableListOf()
    private var styles: YSArray<YSSet<String>> = mutableListOf()
    open override fun clearFormatting(from: Int, to: Int): Unit {
        for (i in (from until to step 1)) {
            this.styles[i] = YSSet<String>()
        }
    }

    open override fun setStrong(from: Int, to: Int): Unit {
        for (i in (from until to step 1)) {
            this.styles[i].add("strong")
        }
    }

    open override fun setItalic(from: Int, to: Int): Unit {
        for (i in (from until to step 1)) {
            this.styles[i].add("em")
        }
    }

    open override fun appendText(index: Int, text: String): Unit {
        val newBody: YSArray<String> = mutableListOf()
        val newStyles: YSArray<YSSet<String>> = mutableListOf()
        if (index < 1) {
            for (i in (0 as Int until text.length step 1)) {
                val symbol = text.slice(i, i + 1)
                newBody.add(symbol)
                if (this.styles.size > index) {
                    newStyles.add(this.styles[index])
                } else {
                    newStyles.add(YSSet<String>())
                }
            }
            for (i in (0 until this.symbols.size step 1)) {
                newBody.add(this.symbols[i])
                newStyles.add(this.styles[i])
            }
        } else {
            for (i in (0 until this.symbols.size step 1)) {
                if (i == index) {
                    for (j in (0 as Int until text.length step 1)) {
                        val symbol = text.slice(j, j + 1)
                        newBody.add(symbol)
                        newStyles.add(this.styles[i - 1])
                    }
                }
                newBody.add(this.symbols[i])
                newStyles.add(this.styles[i])
            }
        }
        this.symbols = newBody
        this.styles = newStyles
    }

    open fun getBody(): String {
        var newBody = ""
        var prevStyles: YSSet<String> = YSSet<String>()
        var appendSymbol: String
        for (i in (0 as Int until this.symbols.size step 1)) {
            appendSymbol = ""
            for (style in this.styles[i].values()) {
                if (!prevStyles.has(style)) {
                    appendSymbol += "<${style}>"
                }
            }
            for (style in prevStyles.values()) {
                if (!this.styles[i].has(style)) {
                    appendSymbol += "</${style}>"
                }
            }
            appendSymbol += this.symbols[i]
            newBody += appendSymbol
            prevStyles = this.styles[i]
        }
        return newBody
    }

    open fun clear(): Unit {
        this.symbols = mutableListOf()
        this.styles = mutableListOf()
    }

    open fun getSymbols(): YSArray<String> {
        return this.symbols
    }

    open fun getStyles(): YSArray<YSSet<String>> {
        return this.styles
    }

}

