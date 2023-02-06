package ru.yandex.travel.yt_lucene_index

import com.google.protobuf.Message
import org.apache.lucene.document.Document

class TestLuceneIndexBuilder<ProtoClass : Message> : LuceneIndexBuilder<ProtoClass> {
    private var luceneData: List<ProtoClass> = emptyList()

    fun setLuceneData(luceneData: List<ProtoClass>): TestLuceneIndexBuilder<ProtoClass> {
        this.luceneData = luceneData

        return this
    }

    override fun build(
        params: YtLuceneIndexParams,
        luceneIndexName: String,
        documentProducer: (row: ProtoClass) -> Iterable<Document>
    ): YtLuceneIndex {
        return TestLuceneIndex(params, luceneIndexName, luceneData, documentProducer)
    }
}
