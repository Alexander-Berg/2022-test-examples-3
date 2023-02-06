package ru.yandex.market.contentmapping.services.fileprocessing

import org.assertj.core.api.Assertions
import org.junit.Test
import java.util.*

class PicturesUrlExtractorTest {
    @Test
    fun extract() {
        val input = "https://cdn3.static1-sima-land.com/items/3292244/1/700-nw.jpg, " +
                "https://cdn3.static1-sima-land.com/items/3292244/2/700-nw.jpg; " +
                "https://cdn3.static1-sima-land.com/items/3292244/3/700-nw.jpg\t" +
                "https://cdn3.static1-sima-land.com/items/3292244/4/700-nw.jpg,; " +
                "https://cdn3.static1-sima-land.com/items/3292244/5/700-nw.jpg;, "
        val picturesUrlExtractor = PicturesUrlExtractor("[,;\t]")
        val result = picturesUrlExtractor.extract(input)
        val expected: MutableList<String> = ArrayList()
        expected.add("https://cdn3.static1-sima-land.com/items/3292244/1/700-nw.jpg")
        expected.add("https://cdn3.static1-sima-land.com/items/3292244/2/700-nw.jpg")
        expected.add("https://cdn3.static1-sima-land.com/items/3292244/3/700-nw.jpg")
        expected.add("https://cdn3.static1-sima-land.com/items/3292244/4/700-nw.jpg")
        expected.add("https://cdn3.static1-sima-land.com/items/3292244/5/700-nw.jpg")
        Assertions.assertThat(result).containsExactlyInAnyOrderElementsOf(expected)
    }
}
