package ru.yandex.travel.yt_lucene_index

import com.google.protobuf.Message
import org.apache.lucene.analysis.custom.CustomAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.StoredField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.SearcherFactory
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.store.Directory
import org.apache.lucene.store.MMapDirectory
import org.slf4j.LoggerFactory
import ru.yandex.travel.yt_lucene_index.YtLuceneIndex.IndexUpdateHandler
import ru.yandex.travel.yt_lucene_index.YtLuceneIndex.SearchExecutor
import java.io.IOException
import java.lang.Exception
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class TestLuceneIndex<ProtoClass : Message>(
    params: YtLuceneIndexParams,
    private val name: String,
    private val luceneData: List<ProtoClass> = emptyList(),
    private val documentProducer: (row: ProtoClass) -> Iterable<Document>
) : YtLuceneIndex {
    private val log = LoggerFactory.getLogger(this.javaClass.name + "[" + name + "]")

    private val FIELD_SYSTEM_DOCUMENT = "systemDocument"

    private var indexPath: Path = Path.of(params.indexPath).toAbsolutePath().normalize()
    private val indexDirectory: Directory

    init {
        try {
            val mMapDirectory = MMapDirectory.open(indexPath) as MMapDirectory
            mMapDirectory.preload = params.isPreload
            indexDirectory = mMapDirectory
        } catch (e: IOException) {
            log.error("Failed to open lucene index directory {}", indexPath, e)
            throw RuntimeException(e)
        }
    }

    private val isReady = AtomicBoolean(false)
    private val searcherManagerRef = AtomicReference<SearcherManager?>()
    private val indexUpdateHandler = AtomicReference<IndexUpdateHandler>()
    private var keepIndexOpened = true
    private val indexNumDocs = AtomicInteger(0)

    init {
        CustomAnalyzer.builder().withTokenizer("standard").build().use { analyzer ->
            IndexWriter(indexDirectory, IndexWriterConfig(analyzer)
                .setOpenMode(CREATE)
                .setCommitOnClose(false)
            ).use { indexWriter ->
                val systemDoc = Document()
                systemDoc.add(LongPoint(FIELD_SYSTEM_DOCUMENT, 1)) // For searching
                systemDoc.add(StoredField(FIELD_SYSTEM_DOCUMENT, 1)) // For checking in getAllDocuments
                indexWriter.addDocument(systemDoc)
                val loadedRows = AtomicInteger(0)
                val loadedDocs = AtomicInteger(0)
                luceneData.forEach {
                    try {
                        for (doc in documentProducer(it)) {
                            loadedDocs.incrementAndGet()
                            indexWriter.addDocument(doc)
                        }
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }
                log.info("Loaded from YT: {} rows and {} documents", loadedRows.get(), loadedDocs.get())
                indexWriter.forceMerge(1)
                indexWriter.commit()
            }
        }

        start()
    }

    override fun start() {
        reopenIndex()
    }

    override fun stop() {
        log.debug("Stop started")
        closeSearcherManager()
        try {
            log.info("Closing IndexDirectory")
            indexDirectory.close()
        } catch (e: IOException) {
            log.error("Failed to close IndexDirectory", e)
        }
        log.info("Stop finished")
    }

    private fun closeSearcherManager() {
        val sm = searcherManagerRef.getAndSet(null)
        if (sm != null) {
            log.info("Closing SearcherManager")
            try {
                sm.close()
            } catch (e: IOException) {
                log.error("Failed to close SearcherManager", e)
            }
        }
    }

    override fun <T> search(executor: SearchExecutor<T>): T {
        val sm = searcherManagerRef.get() ?: throw IllegalStateException("Lucene index '$name' is not ready")
        var searcher: IndexSearcher? = null

        return try {
            searcher = sm.acquire()
            executor.execute(searcher)
        } catch (e: IOException) {
            throw IllegalStateException("Index '$name' is not readable", e)
        } finally {
            try {
                sm.release(searcher)
            } catch (e: IOException) {
                log.error("Failed to release IndexSearcher", e)
            }
        }
    }

    override fun forEachDocument(consumer: Consumer<Document>) {
        search { searcher: IndexSearcher ->
            val topDocs = searcher.search(MatchAllDocsQuery(), searcher.indexReader.numDocs())
            for (scoreDoc in topDocs.scoreDocs) {
                val doc = searcher.doc(scoreDoc.doc)
                if (doc.getField(FIELD_SYSTEM_DOCUMENT) == null) {
                    consumer.accept(doc)
                }
            }
            null
        }
    }

    override fun isReady(): Boolean {
        return isReady.get()
    }

    private fun reopenIndex() {
        val started = System.currentTimeMillis()
        log.info("Reopening index at {}", indexPath)
        var searcher: IndexSearcher? = null
        var sm: SearcherManager? = null
        try {
            sm = searcherManagerRef.get()?.also {
                it.maybeRefreshBlocking()
            } ?: SearcherManager(indexDirectory, SearcherFactory())
            searcher = sm.acquire()

            val query = LongPoint.newExactQuery(FIELD_SYSTEM_DOCUMENT, 1)
            val topDocs = searcher.search(query, 1)

            check(topDocs.totalHits >= 1) { "Failed to find system document in index" }

            indexNumDocs.set(searcher.indexReader.numDocs() - 1) // 1 is system doc
            searcherManagerRef.set(sm)
            log.info("Index reopened in ${System.currentTimeMillis() - started} ms, has ${indexNumDocs.get()} documents")

            try {
                indexUpdateHandler.get()?.process()
            } catch (e: Exception) {
                log.error("Exception during index update handler", e)
                throw e
            }
            isReady.set(true)
            if (!keepIndexOpened) {
                log.info("Closing index")
                closeSearcherManager()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            if (sm != null && searcher != null) {
                try {
                    sm.release(searcher)
                } catch (e: IOException) {
                    log.error("Failed to release IndexSearcher", e)
                }
            }
        }
    }

    override fun setIndexUpdateHandler(indexUpdateHandler: IndexUpdateHandler) {
        this.indexUpdateHandler.set(indexUpdateHandler)
    }

    override fun setKeepIndexOpened(keepIndexOpened: Boolean) {
        this.keepIndexOpened = keepIndexOpened
    }
}
