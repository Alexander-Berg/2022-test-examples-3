package ru.yandex.market.abo.core.premod.item.creator;

import java.util.HashSet;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author komarovns
 * @date 27.08.2020
 */
class PremodItemCreatorTest extends EmptyTest {
    @Autowired
    List<PremodItemCreator> premodItemCreators;

    @Test
    public void uniqueTypesTest() {
        assertFalse(premodItemCreators.isEmpty());
        var set = new HashSet<>();
        StreamEx.of(premodItemCreators)
                .map(PremodItemCreator::checkType)
                .forEach(type -> assertTrue(set.add(type), type + " occurs more than once"));
    }
}
