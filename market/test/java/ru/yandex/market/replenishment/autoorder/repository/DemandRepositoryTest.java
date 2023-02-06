package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandStatus;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandDTO;
import ru.yandex.market.replenishment.autoorder.repository.postgres.DemandDTORepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
public class DemandRepositoryTest extends FunctionalTest {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    @DbUnitDataSet(before = "DemandRepositoryTest.UpdateOrderInfo.before.csv",
            after = "DemandRepositoryTest.UpdateOrderInfo.after.csv")
    public void testUpdateOrderInfo() {
        try (final SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)) {
            final DemandDTORepository mapper = sqlSession.getMapper(DemandDTORepository.class);
            mapper.updateOrderInfo(LocalDateTime.of(2018, 11, 10, 0, 0),
                Set.of(DemandType.TYPE_1P));
        }
    }

    @Test
    @DbUnitDataSet(before = "DemandRepositoryTest.testGetForUnion.before.csv")
    public void testGetDemandToUnite() {
        List<DemandDTO> demands;
        DemandDTO demand;

        try (final SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)) {
            final DemandDTORepository mapper = sqlSession.getMapper(DemandDTORepository.class);
            demands = mapper.getDemandsForUnion(DemandType.TYPE_1P, 1L);
            demand = mapper.getDemandById(DemandType.TYPE_1P, 1L);
        }

        assertNotNull(demands);
        assertEquals(3, demands.size());

        assertNotNull(demand);
        assertFalse(demands.contains(demand));

        demands.forEach(item -> assertEquals(DemandStatus.NEW, item.getStatus()));
    }

    @Test
    @DbUnitDataSet(before = "DemandRepositoryTest.before.csv")
    public void testGetEmptyDemandToUnite() {
        List<DemandDTO> demands;
        try (final SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)) {
            final DemandDTORepository mapper = sqlSession.getMapper(DemandDTORepository.class);
            demands = mapper.getDemandsForUnion(DemandType.TYPE_1P, 5L);
        }

        assertNotNull(demands);
        assertEquals(0, demands.size());
    }

    @Test
    @DbUnitDataSet(before = "DemandRepositoryTest.UpdateResultInfo.before.csv",
            after = "DemandRepositoryTest.UpdateResultInfo.after.csv")
    public void testUpdateDemandsResultInfo() {
        try (final SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)) {
            final DemandDTORepository mapper = sqlSession.getMapper(DemandDTORepository.class);
            mapper.updateDemandResultsInfo(DemandType.TYPE_1P, Arrays.asList(1L, 3L, 4L));
        }
    }

    @Test
    @DbUnitDataSet(before = "DemandRepositoryTest.UpdateWarehouseId.before.csv",
            after = "DemandRepositoryTest.UpdateWarehouseId.after.csv")
    public void testUpdateWarehouseId() {
        try (final SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)) {
            final DemandDTORepository mapper = sqlSession.getMapper(DemandDTORepository.class);
            mapper.updateWarehouseId(1L, 172L);
        }
    }
}
