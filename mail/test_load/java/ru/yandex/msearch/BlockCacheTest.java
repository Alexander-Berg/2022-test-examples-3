package ru.yandex.msearch;

import java.lang.reflect.Field;
import java.nio.file.Path;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.codecs.yandex.YandexPostingsReader;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.ZstdCompressor;
import org.apache.lucene.util.BitVector;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class BlockCacheTest extends TestBase {
    private <T> void setField(final T obj, final String name, final Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    public void testCachingSkiptable() throws Exception {
        Path path = resource("skip_table_cache_test_8is_0.frqy");

        //NIOFSDirectory dir = NIOFSDirectory.get(new File("/home/vonidu/Downloads/block_test_index2/index/default/0"));
        NIOFSDirectory dir = NIOFSDirectory.get(path.getParent().toFile());
        YandexPostingsReader reader = new YandexPostingsReader(
            new ZstdCompressor(3),
            dir,
            new SegmentInfo("skip_table_cache_test_8is", 853477, dir, false, false, null, false),
            1024,
            "0"
        );

        FieldInfos infos = new FieldInfos();
        infos.add("id", true, false, false, false, true, false, true);
        FieldInfo fieldInfo = infos.fieldInfo("id");

        setField(reader, "blockSize", 4096);
        setField(reader, "skipInterval", 16);
        setField(reader,"maxSkipLevels", 10);
        setField(reader, "skipMinimum", 16);

        YandexPostingsReader.YandexTermState ystate = new YandexPostingsReader.YandexTermState();

        setField(ystate, "skipBlockStart", 16106288L);
        setField(ystate,"skipBlockOffset", 1L);
        setField(ystate, "proxBlockStart", 0L);
        setField(ystate, "proxBlockOffset", 0L);

        ystate.freqBlockStart = 16103428;
        ystate.freqBlockOffset = 3900;
        ystate.fullTerm = false;
        ystate.bytesReader = null;
        ystate.termPointer = 1540;
        ystate.term = new BytesRef("b1ggh9onj7ljr7m9cici#1631202061384209");
        ystate.blockFirstTerm = new BytesRef("b1ggh9onj7ljr7m9cici#1631198791826418");
        ystate.seekStatus = null;
        ystate.docFreq = 4228;
        ystate.totalTermFreq = -1;
        ystate.termCount = 119;
        ystate.blockFilePointer = 39173981;
        ystate.blockTermCount = 330;
        ystate.ord = -1;
        DocsEnum docsEnum = reader.docs(fieldInfo, ystate, new BitVector(853477), null, true);
        docsEnum.advance(853392);

        ystate = new YandexPostingsReader.YandexTermState();
        setField(ystate, "skipBlockStart", 16122446L);
        setField(ystate,"skipBlockOffset", 1L);
        setField(ystate, "proxBlockStart", 0L);
        setField(ystate, "proxBlockOffset", 0L);
        ystate.freqBlockStart = 16118019;
        ystate.freqBlockOffset = 3252;
        ystate.fullTerm = false;
        ystate.bytesReader = null;
        ystate.termPointer = 3346;
        ystate.term = new BytesRef("b1ggh9onj7ljr7m9cici#1631286932420024");
        ystate.blockFirstTerm = new BytesRef("b1ggh9onj7ljr7m9cici#1631281665034050");
        ystate.seekStatus = null;
        ystate.docFreq = 51048;
        ystate.totalTermFreq = -1;
        ystate.termCount = 265;
        ystate.blockFilePointer = 39206051;
        ystate.blockTermCount = 326;
        ystate.ord = -1;

        docsEnum = reader.docs(fieldInfo, ystate, new BitVector(853477), docsEnum, true);
        docsEnum.advance(853392);

        ystate.freqBlockStart = 16124734;
        ystate.freqBlockOffset = 189;
        setField(ystate, "proxBlockStart", 0L);
        setField(ystate, "proxBlockOffset", 0L);
        setField(ystate, "skipBlockStart", 16127143L);
        setField(ystate, "skipBlockOffset", 1L);
        ystate.fullTerm = false;
        ystate.bytesReader = null;
        ystate.termPointer = 4029;
        ystate.term = new BytesRef("b1ggh9onj7ljr7m9cici#1631288474235846");
        ystate.blockFirstTerm = new BytesRef("b1ggh9onj7ljr7m9cici#1631281665034050");
        ystate.seekStatus = null;
        ystate.docFreq = 39072;
        ystate.totalTermFreq = -1;
        ystate.termCount = 319;
        ystate.blockFilePointer = 39206051;
        ystate.blockTermCount = 326;
        ystate.ord = -1;

        docsEnum = reader.docs(fieldInfo, ystate, new BitVector(853477), docsEnum, true);
        docsEnum.advance(853392);
    }
}
