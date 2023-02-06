package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MercuryHashDao;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

public class MercuryHashRepositoryTest extends MdmBaseDbTestClass {
    @Autowired
    private MercuryHashRepository mercuryHashRepository;

    @Test
    public void testBatchIterator() {
        MercuryHashDao hash1 = MercuryHashDao.create("1", 10, "Processor1");
        MercuryHashDao hash2 = MercuryHashDao.create("2", 11, "Processor1");
        MercuryHashDao hash3 = MercuryHashDao.create("3", 12, "Processor2");
        MercuryHashDao hash4 = MercuryHashDao.create("4", 13, "Processor1");
        MercuryHashDao hash5 = MercuryHashDao.create("5", 14, "Processor1");
        MercuryHashDao hash6 = MercuryHashDao.create("6", 15, "Processor2");
        MercuryHashDao hash7 = MercuryHashDao.create("7", 16, "Processor1");
        MercuryHashDao hash8 = MercuryHashDao.create("8", 17, "Processor1");
        MercuryHashDao hash9 = MercuryHashDao.create("9", 18, "Processor1");
        List<MercuryHashDao> allHashes = new ArrayList<>(
            List.of(hash1, hash2, hash3, hash4, hash5, hash6, hash7, hash8, hash9)
        );
        Collections.shuffle(allHashes);
        allHashes.forEach(mercuryHashRepository::insertOrUpdate);

        Iterator<List<MercuryHashDao>> processor1Iterator =
            mercuryHashRepository.allHashesBatchIterator("Processor1", 2, null);

        Assertions.assertThat(processor1Iterator.hasNext()).isTrue();
        //noinspection ConstantConditions
        Assertions.assertThat(processor1Iterator.hasNext()).isTrue();
        Assertions.assertThat(processor1Iterator.next()).containsExactly(hash1, hash2);
        Assertions.assertThat(processor1Iterator.hasNext()).isTrue();
        Assertions.assertThat(processor1Iterator.next()).containsExactly(hash4, hash5);
        Assertions.assertThat(processor1Iterator.next()).containsExactly(hash7, hash8);
        Assertions.assertThat(processor1Iterator.next()).containsExactly(hash9);
        Assertions.assertThat(processor1Iterator.hasNext()).isFalse();
        //noinspection ConstantConditions
        Assertions.assertThat(processor1Iterator.hasNext()).isFalse();
        Assertions.assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(processor1Iterator::next);

        Iterator<List<MercuryHashDao>> processor2Iterator =
            mercuryHashRepository.allHashesBatchIterator("Processor2", 59, "5");
        Assertions.assertThat(processor2Iterator.hasNext()).isTrue();
        //noinspection ConstantConditions
        Assertions.assertThat(processor2Iterator.hasNext()).isTrue();
        Assertions.assertThat(processor2Iterator.next()).containsExactly(hash6);
        Assertions.assertThat(processor2Iterator.hasNext()).isFalse();
        //noinspection ConstantConditions
        Assertions.assertThat(processor2Iterator.hasNext()).isFalse();
        Assertions.assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(processor2Iterator::next);
    }
}
