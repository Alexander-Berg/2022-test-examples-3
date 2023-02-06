package ru.yandex.market.mboc.common.lighmapper;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.lightmapper.GenericMapperRepositoryImpl;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author shadoff
 * created on 2/7/22
 */
public class GenericMapperRepositoryTest extends BaseDbTestClass {
    @Autowired
    private List<GenericMapperRepositoryImpl<?, ?>> repositories;

    @Test
    public void testColumnsInMapper() {
        // test all generic repositories composite mappers
        repositories.forEach(GenericMapperRepositoryImpl::testColumnsInMapper);
    }
}
