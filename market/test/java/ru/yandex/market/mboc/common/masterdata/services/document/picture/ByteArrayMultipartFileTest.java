package ru.yandex.market.mboc.common.masterdata.services.document.picture;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class ByteArrayMultipartFileTest {

    private static final Long SEED = 9393L;
    private static final int INT_BOUND = 10;
    private String name;
    private byte[] bytes;
    private ByteArrayMultipartFile file;

    @Before
    public void setUp() {
        EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();
        generateFile(random);
    }

    private void generateFile(EnhancedRandom random) {
        bytes = new byte[random.nextInt(INT_BOUND)];
        random.nextBytes(bytes);
        name = random.nextObject(String.class);
        file = new ByteArrayMultipartFile(name, bytes);
    }

    @Test
    public void whenGettingOriginalNameShouldReturnFullName() {
        Assertions.assertThat(file.getOriginalFilename()).isEqualTo(name);
    }

    @Test
    public void whenGettingBytesShouldReturnBytesField() {
        Assertions.assertThat(file.getBytes()).containsExactly(bytes);
    }
}
