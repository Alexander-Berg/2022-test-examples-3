package ru.yandex.market.abo.core.premod.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.premod.model.PremodShopCategories;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author agavrikov
 * @date 20.05.18
 */
public class PremodShopCategoriesRepoTest extends EmptyTest {

    private static final String CATEGORY_DELIMITER = ",";

    @Autowired
    PremodShopCategoriesRepo premodShopCategoriesRepo;

    @Test
    public void testRepo() throws Exception {
        premodShopCategoriesRepo.save(initPremodShopCategories());
        assertEquals(Arrays.asList(1L, 2L, 3L),
            Arrays.stream(new String(
                premodShopCategoriesRepo.getOne(1L).getShopCategories()).split(CATEGORY_DELIMITER)
            ).map(Long::valueOf).collect(Collectors.toList())
        );
        List<Long> premodCategories = Arrays.asList(1L, 2L, 4L);
        premodShopCategoriesRepo.save(new PremodShopCategories(1L,
                premodCategories.stream().map(Object::toString).collect(Collectors.joining(CATEGORY_DELIMITER))));
        assertEquals(Arrays.asList(1L, 2L, 4L),
                Arrays.stream(new String(
                        premodShopCategoriesRepo.getOne(1L).getShopCategories()).split(CATEGORY_DELIMITER)
                ).map(Long::valueOf).collect(Collectors.toList())
        );
    }

    private static PremodShopCategories initPremodShopCategories() {
        return new PremodShopCategories(1L, "1,2,3");
    }
}

