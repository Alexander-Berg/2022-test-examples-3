package ru.yandex.market.deepmind.tms.executors;

import java.util.Collection;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.MskuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.MskuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.services.DeepmindConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.tms.executors.AutoMoveMskuToAnotherWarehouseRobot.AUTO_MOVE_MSKU_COMMENT;

public class AutoMoveMskuToAnotherWarehouseRobotTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {

    private static final Long SAMSUNG = 153061L;
    private static final Long APPLE = 153043L;

    private static final Long HEADPHONES = 90555L;    // Наушники и Bluetooth-гарнитуры
    private static final Long PLAYERS = 90560L;     // Портативные цифровые плееры
    private static final Long NOTEBOOKS = 91013L;     // Ноутбуки
    private static final Long WIRES = 91074L;     // Компьютерные кабели, разъемы, переходники

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private MskuRepository deepmindMskuRepository;
    @Autowired
    private MskuAvailabilityChangedHandler mskuAvailabilityChangedHandler;
    @Autowired
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;

    private AutoMoveMskuToAnotherWarehouseRobot robot;

    @Before
    public void setUp() {
        robot = new AutoMoveMskuToAnotherWarehouseRobot(
            namedParameterJdbcTemplate,
            mskuAvailabilityChangedHandler,
            transactionHelper
        );
    }

    @Test
    public void testExecute() {
        var msku1 = deepmindMskuRepository.save(newMsku(1, HEADPHONES, SAMSUNG));
        var msku2 = deepmindMskuRepository.save(newMsku(2, PLAYERS, APPLE));
        clearQueue();

        robot.execute();

        Assertions.assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "available", "createdLogin", "comment",
                "blockReasonKey")
            .containsExactlyInAnyOrder(
                // В Софино возим
                matrix(msku1, SOFINO_ID, true, DeepmindConstants.APPLE_SAMSUNG_ROBOT, AUTO_MOVE_MSKU_COMMENT),
                matrix(msku2, SOFINO_ID, true, DeepmindConstants.APPLE_SAMSUNG_ROBOT, AUTO_MOVE_MSKU_COMMENT),

                // На Маршрут всё заблокировано
                matrix(msku1, MARSHRUT_ID, false, DeepmindConstants.APPLE_SAMSUNG_ROBOT, AUTO_MOVE_MSKU_COMMENT),
                matrix(msku2, MARSHRUT_ID, false, DeepmindConstants.APPLE_SAMSUNG_ROBOT, AUTO_MOVE_MSKU_COMMENT)
            );

        assertThatTaskQueueContainsMskuIds(
            msku1.getId(),
            msku2.getId()
        );
    }

    @Test
    public void testNoMsku() {
        deepmindMskuRepository.save(newMsku(1, 22, 222));
        clearQueue();
        robot.execute();
        assertThat(mskuAvailabilityMatrixRepository.findAll()).isEmpty();
        assertThatTaskQueueIsEmpty();
    }

    @Test
    public void testIgnoreAnotherMskus() {
        var msku1 = deepmindMskuRepository.save(newMsku(1, NOTEBOOKS, APPLE));
        deepmindMskuRepository.save(newMsku(2, NOTEBOOKS, 111));
        deepmindMskuRepository.save(newMsku(3, 11, SAMSUNG));
        deepmindMskuRepository.save(newMsku(4, 22, 222));
        clearQueue();

        robot.execute();

        Assertions.assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "available", "createdLogin", "blockReasonKey")
            .containsExactlyInAnyOrder(
                matrix(msku1, SOFINO_ID, true, DeepmindConstants.APPLE_SAMSUNG_ROBOT),
                matrix(msku1, MARSHRUT_ID, false, DeepmindConstants.APPLE_SAMSUNG_ROBOT)
            );

        assertThatTaskQueueContainsMskuIds(msku1.getId());
    }

    @Test
    public void testNotChangePrevAvailability() {
        var msku = deepmindMskuRepository.save(newMsku(1, WIRES, APPLE));
        // товар был запрещен в "Софино" и разрешён на "Маршрут" другим пользователем.
        var matrix1 = mskuAvailabilityMatrixRepository.save(matrix(msku, SOFINO_ID, false, "test_user"));
        var matrix2 = mskuAvailabilityMatrixRepository.save(matrix(msku, MARSHRUT_ID, true, "test_user"));
        clearQueue();

        robot.execute();

        // ничего не изменится
        Assertions.assertThat(mskuAvailabilityMatrixRepository.findAll())
            .containsExactlyInAnyOrder(
                matrix1, matrix2
            );

        assertThatTaskQueueIsEmpty();
    }

    @Test
    public void testShouldNotAffectOtherWarehouses() {
        var msku = deepmindMskuRepository.save(newMsku(1, WIRES, SAMSUNG));
        var matrix1 = mskuAvailabilityMatrixRepository.save(matrix(msku, TOMILINO_ID, false, "test_user"));
        var matrix2 = mskuAvailabilityMatrixRepository.save(matrix(msku, ROSTOV_ID, true, "test_user"));
        clearQueue();

        robot.execute();

        Assertions.assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "available", "createdLogin", "blockReasonKey")
            .containsExactlyInAnyOrder(
                matrix1, matrix2, // другие склады не меняются
                matrix(msku, SOFINO_ID, true, DeepmindConstants.APPLE_SAMSUNG_ROBOT),
                matrix(msku, MARSHRUT_ID, false, DeepmindConstants.APPLE_SAMSUNG_ROBOT)
            );

        assertThatTaskQueueContainsMskuIds(msku.getId());
    }

    @Test
    public void testDontAddDuplicates() {
        var msku = deepmindMskuRepository.save(newMsku(1, HEADPHONES, SAMSUNG));

        clearQueue();
        robot.execute();
        assertThat(mskuAvailabilityMatrixRepository.findAll()).hasSize(2);
        assertThatTaskQueueContainsMskuIds(msku.getId());

        clearQueue();
        robot.execute();
        assertThat(mskuAvailabilityMatrixRepository.findAll()).hasSize(2);
        assertThatTaskQueueIsEmpty();
    }

    private void assertThatTaskQueueContainsMskuIds(Long... mskuIds) {
        var tasks = getQueueTasksOfType(MskuAvailabilityChangedTask.class);
        var mskuIdsToRefresh = tasks.stream().map(MskuAvailabilityChangedTask::getMskuIds)
            .flatMap(Collection::stream).collect(Collectors.toList());
        Assertions.assertThat(mskuIdsToRefresh)
            .containsExactlyInAnyOrder(mskuIds);
    }

    private void assertThatTaskQueueIsEmpty() {
        var tasks = getQueueTasksOfType(MskuAvailabilityChangedTask.class);
        Assertions.assertThat(tasks).isEmpty();
    }

    private Msku newMsku(long id, long categoryId, long vendorId) {
        return new Msku()
            .setId(id)
            .setTitle("msku_of_category_" + categoryId)
            .setCategoryId(categoryId)
            .setVendorId(vendorId)
            .setSkuType(SkuTypeEnum.SKU);
    }

    private MskuAvailabilityMatrix matrix(Msku msku, long warehouseId, boolean available,
                                          String login, String comment) {
        return matrix(msku, warehouseId, available, login).setComment(comment);
    }

    private MskuAvailabilityMatrix matrix(Msku msku, long warehouseId, boolean available, String login) {
        return new MskuAvailabilityMatrix()
            .setMarketSkuId(msku.getId())
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setCreatedLogin(login)
            .setBlockReasonKey(BlockReasonKey.OTHER);
    }

}
