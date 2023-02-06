package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.cccode.Cis;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCodeType;
import ru.yandex.market.mboc.common.masterdata.model.cccode.MdmParamMarkupState;
import ru.yandex.market.mboc.common.web.CustomsCommCodeLite;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class CustomsCommCodeRepositoryImplTest extends MdmBaseDbTestClass {

    @Autowired
    private CustomsCommCodeRepository repository;
    @Autowired
    private MdmGoodGroupRepository mdmGoodGroupRepository;

    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(1660);
        repository.deleteAll();
    }

    @Test
    public void testSimpleInsert() {
        CustomsCommCode record = record();
        repository.insert(record);
        CustomsCommCode found = repository.findById(record.getId());
        assertThat(record).isEqualTo(found);
    }

    @Test
    public void testSimpleInsertWithNullSettings() {
        CustomsCommCode record = record()
            .setMercury(null)
            .setHonestSign(null);
        repository.insert(record);
        CustomsCommCode found = repository.findById(record.getId());
        assertThat(record).isEqualTo(found);
    }

    @Test
    public void testMultipleInsert() {
        List<CustomsCommCode> records = List.of(record(), record().setId(1), record().setId(2));
        repository.insertBatch(records);
        List<CustomsCommCode> found = repository.findByIds(keys(records));
        assertThat(found).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testMultipleInsert2() {
        List<CustomsCommCode> records = List.of(record(), record().setId(1), record().setId(2));
        repository.insertBatch(records);
        List<CustomsCommCode> found = repository.findByIds(keys(records));
        assertThat(found).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testSimpleDelete() {
        CustomsCommCode record = record();
        repository.insert(record);
        repository.delete(record);
        assertThat(repository.totalCount()).isZero();
    }

    @Test
    public void testMultipleDelete() {
        List<CustomsCommCode> records = List.of(record(), record().setId(1), record().setId(2));
        repository.insertBatch(records);
        repository.delete(keys(records));
        assertThat(repository.totalCount()).isZero();
    }

    @Test
    public void testSimpleUpdate() {
        CustomsCommCode record = record();
        record.setHonestSign(new MdmParamMarkupState().setCis(Cis.REQUIRED));
        record = repository.insert(record);

        record.setHonestSign(new MdmParamMarkupState().setCis(Cis.OPTIONAL));
        repository.update(record);

        assertThat(repository.findById(record.getId())).isEqualTo(record);
    }

    @Test
    public void testDeleteWithHierarchy() {
        CustomsCommCode parent1 = repository.insert(record());
        CustomsCommCode parent2 = repository.insert(record());
        CustomsCommCode child11 = repository.insert(record().setParentId(parent1.getId()));
        CustomsCommCode child12 = repository.insert(record().setParentId(parent1.getId()));
        CustomsCommCode child111 = repository.insert(record().setParentId(child11.getId()));
        CustomsCommCode child21 = repository.insert(record().setParentId(parent2.getId()));
        CustomsCommCode child22 = repository.insert(record().setParentId(parent2.getId()));

        repository.deleteSubTree(parent1.getId());
        assertThat(repository.findAll()).containsExactlyInAnyOrder(
            parent2, child21, child22
        );
    }

    @Test
    public void testFindByCodeCanFindOnlyCode() {
        // given
        CustomsCommCode code = repository.insert(record(CustomsCommCodeType.CODE));
        CustomsCommCode group = repository.insert(record(CustomsCommCodeType.GROUP));

        // when
        Optional<CustomsCommCode> foundCode = repository.findByCode(code.getCode());
        Optional<CustomsCommCode> foundGroup = repository.findByCode(group.getCode());

        // then
        assertThat(foundCode).contains(code);
        assertThat(foundGroup).isEmpty();
    }

    @Test
    public void testFindAllLiteFoundAll() {
        // given
        CustomsCommCode code = repository.insert(record(CustomsCommCodeType.CODE));
        CustomsCommCode group = repository.insert(record(CustomsCommCodeType.GROUP));

        // when
        List<CustomsCommCodeLite> foundCodes = repository.findAllLite(true);

        // then
        assertThat(foundCodes).extracting(CustomsCommCodeLite::getId)
            .containsExactlyInAnyOrder(code.getId(), group.getId());
    }

    @Test
    public void testFindAllLiteFoundAllExceptGroup() {
        // given
        CustomsCommCode code = repository.insert(record(CustomsCommCodeType.CODE));
        CustomsCommCode group = repository.insert(record(CustomsCommCodeType.GROUP));

        // when
        List<CustomsCommCodeLite> foundCodes = repository.findAllLite(false);

        // then
        assertThat(foundCodes).extracting(CustomsCommCodeLite::getId).containsExactly(code.getId());
    }

    private CustomsCommCode record() {
        return record(CustomsCommCodeType.CODE);
    }

    private CustomsCommCode record(CustomsCommCodeType type) {
        long goodGroupId = mdmGoodGroupRepository.findAll().get(0).getId();
        var result = random.nextObject(CustomsCommCode.class, "id", "parentId")
            .setGoodGroupId(goodGroupId);
        if (type == CustomsCommCodeType.CODE) {
            result
                .setCode(random.nextObject(Integer.class).toString())
                .setType(CustomsCommCodeType.CODE);
        } else {
            result
                .setCode("")
                .setType(CustomsCommCodeType.GROUP);
        }
        result.getModificationInfo().setDqScore(null);
        return result;
    }

    private List<Long> keys(List<CustomsCommCode> records) {
        return records.stream().map(CustomsCommCode::getId).collect(Collectors.toList());
    }
}
