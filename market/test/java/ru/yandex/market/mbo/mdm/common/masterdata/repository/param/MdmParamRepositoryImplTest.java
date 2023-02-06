package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamExternals;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class MdmParamRepositoryImplTest extends MdmBaseDbTestClass {

    @Autowired
    private MdmParamRepository repository;
    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(4796);
    }

    @Test
    public void testSimpleInsert() {
        MdmParam meta = param();
        repository.insert(meta);

        MdmParam found = repository.findById(meta.getId());
        assertThat(found).isEqualTo(meta);
    }

    @Test
    public void testMultipleInsert() {
        List<MdmParam> records = List.of(param(), param(), param());
        repository.insertBatch(records);
        List<MdmParam> found = repository.findByIds(keys(records));
        assertThat(found).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testMultipleInsert2() {
        List<MdmParam> records = List.of(param(), param(), param());
        repository.insertOrUpdateAll(records);
        List<MdmParam> found = repository.findByIds(keys(records));
        assertThat(found).containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testSimpleDelete() {
        int initialCount = repository.totalCount();
        MdmParam record = param();
        repository.insert(record);
        repository.delete(record);
        assertThat(repository.totalCount()).isEqualTo(initialCount);
    }

    @Test
    public void testMultipleDelete() {
        int initialCount = repository.totalCount();
        List<MdmParam> records = List.of(param(), param(), param());
        repository.insertBatch(records);
        repository.delete(keys(records));
        assertThat(repository.totalCount()).isEqualTo(initialCount);
    }

    @Test
    public void testSimpleUpdate() {
        MdmParam record = param();
        record.setTitle("Творобушек");
        repository.insertOrUpdate(record);

        record.setTitle("Бульонные параллелепипеды");
        repository.insertOrUpdate(record);

        assertThat(repository.findById(record.getId())).isEqualTo(record);
    }

    @Test
    public void testMultipleUpdate() {
        Set<Long> existingIgnoredIds = repository.findAll().stream()
            .map(MdmParam::getId).collect(Collectors.toSet());
        List<MdmParam> records = List.of(param(), param(), param());
        foreachEnumerated(records, (i, record) -> {
            record.setTitle("Творобушек #" + i);
        });
        repository.insertOrUpdateAll(records);

        foreachEnumerated(records, (i, record) -> {
            record.setTitle("Бульонный параллелепипед #" + i);
        });
        repository.insertOrUpdateAll(records);

        assertThat(repository.findAll().stream().filter(p -> !existingIgnoredIds.contains(p.getId())))
            .containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testInsertAndUpdateInOneGo() {
        Set<Long> existingIgnoredIds = repository.findAll().stream()
            .map(MdmParam::getId).collect(Collectors.toSet());
        List<MdmParam> records = List.of(param(), param(), param(), param());
        foreachEnumerated(records, (i, record) -> {
            record.setTitle("Творобушек #" + i);
        });
        repository.insertOrUpdateAll(records.stream().limit(2).collect(Collectors.toList()));

        foreachEnumerated(records, (i, record) -> {
            record.setTitle("Бульонный параллелепипед #" + i);
        });
        repository.insertOrUpdateAll(records);

        assertThat(repository.findAll().stream().filter(p -> !existingIgnoredIds.contains(p.getId())))
            .containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void testEmptyEntitiesDecodedEncodedProperly() {
        MdmParam record = param();
        record.setExternals(new MdmParamExternals());
        record.setOptions(List.of());

        repository.insertOrUpdate(record);

        MdmParam found = repository.findById(record.getId());
        assertThat(found).isEqualTo(record);
    }

    private List<Long> keys(List<MdmParam> records) {
        return records.stream().map(MdmParam::getId).collect(Collectors.toList());
    }

    private void foreachEnumerated(Iterable<MdmParam> records, BiConsumer<Integer, MdmParam> action) {
        var iter = records.iterator();
        int count = 0;
        while (iter.hasNext()) {
            action.accept(count++, iter.next());
        }
    }

    private MdmParam param() {
        MdmParamExternals externals = random.nextObject(MdmParamExternals.class, "optionRenders");
        return random.nextObject(MdmParam.class, "externals").setExternals(externals);
    }
}
