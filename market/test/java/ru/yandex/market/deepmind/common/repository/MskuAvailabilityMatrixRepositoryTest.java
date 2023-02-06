package ru.yandex.market.deepmind.common.repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.jooq.exception.NoDataFoundException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository.Filter;
import ru.yandex.market.deepmind.common.utils.SecurityContextAuthenticationUtils;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mboc.common.utils.JooqAuditUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.deepmind.common.utils.TestUtils.mskuAvailability;

/**
 * @author eremeevvo
 * @since 05.07.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MskuAvailabilityMatrixRepositoryTest extends DeepmindBaseDbTestClass {

    private static final String[] IGNORING_FIELDS = {
        "id",
        "createdAt",
        "modifiedAt",
        "deleted",
        "createdLogin",
        "modifiedLogin",
        "blockReasonKey"
    };

    private static final int BATCH_SIZE = 17;
    private static final String TEST_USER = "test_msku_matrix_user";

    @Autowired
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;

    @Autowired
    private MskuRepository deepmindMskuRepository;

    private EnhancedRandom random;

    private Msku msku;

    @Before
    public void setUp() {
        random = TestUtils.createMskuRandom();
        msku = deepmindMskuRepository.save(randomMsku());
    }

    @Test
    public void saveSingle() {
        MskuAvailabilityMatrix created = randomMatrix();
        MskuAvailabilityMatrix saved = mskuAvailabilityMatrixRepository.save(created);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getModifiedAt()).isNotNull();
        /*
         * createdAt может отличаться от modifiedAt. modifiedAt поддерживаться jooq
         * и использует локальную дату и время. При этом createdAt генерируется
         * базой данных.
         */
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved).isEqualToIgnoringGivenFields(created, IGNORING_FIELDS);
    }

    @Test
    public void saveBatch() {
        List<MskuAvailabilityMatrix> values = Stream.generate(this::randomMatrix)
            .limit(BATCH_SIZE).collect(Collectors.toList());
        List<MskuAvailabilityMatrix> saved = mskuAvailabilityMatrixRepository.save(values);

        assertThat(saved)
            .usingElementComparatorIgnoringFields(IGNORING_FIELDS)
            .containsExactlyInAnyOrderElementsOf(values);
    }

    @Test
    public void testLoggedUserUsedToMarkCreate() {
        try {
            SecurityContextAuthenticationUtils.setAuthenticationToken(TEST_USER);

            List<MskuAvailabilityMatrix> records = Stream.generate(this::randomMatrix)
                .limit(BATCH_SIZE).collect(Collectors.toList());
            mskuAvailabilityMatrixRepository.save(records);

            List<MskuAvailabilityMatrix> stored = mskuAvailabilityMatrixRepository.findAll();

            Assertions.assertThat(stored).extracting("createdLogin")
                .containsOnly(TEST_USER);
        } finally {
            SecurityContextAuthenticationUtils.clearAuthenticationToken();
        }
    }

    @Test
    public void testAutoUserUsedToMarkCreate() {
        List<MskuAvailabilityMatrix> records = Stream.generate(this::randomMatrix)
            .limit(BATCH_SIZE).collect(Collectors.toList());
        mskuAvailabilityMatrixRepository.save(records);

        List<MskuAvailabilityMatrix> stored = mskuAvailabilityMatrixRepository.findAll();

        Assertions.assertThat(stored).extracting("createdLogin")
            .containsOnly(JooqAuditUtils.AUTO_USER);
    }

    @Test
    public void testLoggedUserUsedToMarkUpdate() {
        try {
            SecurityContextAuthenticationUtils.setAuthenticationToken(TEST_USER);

            List<MskuAvailabilityMatrix> records = Stream.generate(this::randomMatrix)
                .limit(BATCH_SIZE).collect(Collectors.toList());
            mskuAvailabilityMatrixRepository.save(records);

            List<MskuAvailabilityMatrix> stored = mskuAvailabilityMatrixRepository.findAll();
            mskuAvailabilityMatrixRepository.save(stored);

            List<MskuAvailabilityMatrix> updated = mskuAvailabilityMatrixRepository.findAll();
            Assertions.assertThat(updated).extracting("modifiedLogin")
                .containsOnly(TEST_USER);
        } finally {
            SecurityContextAuthenticationUtils.clearAuthenticationToken();
        }
    }

    @Test
    public void testAutoUserUsedToMarkUpdate() {
        List<MskuAvailabilityMatrix> records = Stream.generate(this::randomMatrix)
            .limit(BATCH_SIZE).collect(Collectors.toList());

        mskuAvailabilityMatrixRepository.save(records);

        List<MskuAvailabilityMatrix> stored = mskuAvailabilityMatrixRepository.findAll();
        mskuAvailabilityMatrixRepository.save(stored);

        List<MskuAvailabilityMatrix> updated = mskuAvailabilityMatrixRepository.findAll();

        Assertions.assertThat(updated).extracting("createdLogin")
            .containsOnly(JooqAuditUtils.AUTO_USER);
    }

    @Test
    public void delete() {
        MskuAvailabilityMatrix saved = mskuAvailabilityMatrixRepository.save(randomMatrix());
        long id = saved.getId();
        mskuAvailabilityMatrixRepository.delete(Collections.singleton(id));

        List<MskuAvailabilityMatrix> found = mskuAvailabilityMatrixRepository.find(Filter.all());
        assertThat(found).isEmpty();

        assertThatThrownBy(() -> mskuAvailabilityMatrixRepository.getById(id))
            .isInstanceOf(NoDataFoundException.class);

        assertThat(mskuAvailabilityMatrixRepository.getByIds(Collections.singleton(id))).isEmpty();
    }

    @Test
    public void filter() {
        List<MskuAvailabilityMatrix> values = Stream.iterate(msku.getId() + 1, i -> i + 1L)
            .limit(BATCH_SIZE)
            .map(mskuId -> {
                Msku msku1 = deepmindMskuRepository.save(randomMsku().setId(mskuId));
                return randomMatrix().setMarketSkuId(msku1.getId());
            })
            .collect(Collectors.toList());

        mskuAvailabilityMatrixRepository.save(values);
        final long mskuId = values.get(0).getMarketSkuId();

        List<MskuAvailabilityMatrix> found = mskuAvailabilityMatrixRepository.find(new Filter()
            .setMskuId(mskuId)
            .setAvailable(true)
        );

        assertThat(found).hasSize(1);
        assertThat(found).extracting(MskuAvailabilityMatrix::getMarketSkuId).containsOnly(mskuId);
    }


    @Test
    public void toDateInclusive() {
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(1);

        mskuAvailabilityMatrixRepository.save(randomMatrix().setFromDate(fromDate).setToDate(toDate));

        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(fromDate))).hasSize(1);
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(toDate))).hasSize(1);
    }

    @Test
    public void fromAndToDatesCanBeTheSame() {
        LocalDate date = LocalDate.now();

        mskuAvailabilityMatrixRepository.save(randomMatrix().setFromDate(date).setToDate(date));

        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(date))).hasSize(1);
    }

    @Test
    public void fromCannotBeBeforeTo() {
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.minusDays(1);

        assertThatThrownBy(() ->
            mskuAvailabilityMatrixRepository.save(randomMatrix().setFromDate(fromDate).setToDate(toDate))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void filterBetweenDate() {
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(3);

        MskuAvailabilityMatrix testMatrix = mskuAvailabilityMatrixRepository.save(randomMatrix());

        testMatrix = mskuAvailabilityMatrixRepository.save(testMatrix
            .setFromDate(fromDate)
            .setToDate(toDate));
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(fromDate.minusDays(1))))
            .isEmpty();
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(toDate.plusDays(1))))
            .isEmpty();
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(fromDate)))
            .hasSize(1);
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(toDate)))
            .hasSize(1);

        testMatrix = mskuAvailabilityMatrixRepository.save(testMatrix
            .setFromDate(null)
            .setToDate(toDate));
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(fromDate.minusDays(1))))
            .hasSize(1);
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(toDate.plusDays(1))))
            .isEmpty();
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(fromDate)))
            .hasSize(1);
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(toDate)))
            .hasSize(1);

        testMatrix = mskuAvailabilityMatrixRepository.save(testMatrix
            .setFromDate(fromDate)
            .setToDate(null));
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(fromDate.minusDays(1))))
            .isEmpty();
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(toDate.plusDays(1))))
            .hasSize(1);
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(fromDate)))
            .hasSize(1);
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(toDate)))
            .hasSize(1);


        testMatrix = mskuAvailabilityMatrixRepository.save(testMatrix
            .setFromDate(null)
            .setToDate(null));
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(fromDate.minusDays(1))))
            .hasSize(1);
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(toDate.plusDays(1))))
            .hasSize(1);
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(fromDate)))
            .hasSize(1);
        assertThat(mskuAvailabilityMatrixRepository.find(targetDate(toDate)))
            .hasSize(1);
    }

    private Filter targetDate(LocalDate betweenDate) {
        return new Filter()
            .setTargetDate(betweenDate);
    }

    @Test
    public void filterWarehouseOptions() {
        MskuAvailabilityMatrix with1 = mskuAvailabilityMatrixRepository.save(randomMatrix().setWarehouseId(1L));
        MskuAvailabilityMatrix with2 = mskuAvailabilityMatrixRepository.save(randomMatrix()
            .setMarketSkuId(deepmindMskuRepository.save(randomMsku()).getId())
            .setWarehouseId(2L));

        assertThat(mskuAvailabilityMatrixRepository.find(
            new Filter().addWarehouseIds(1L)))
            .extracting(MskuAvailabilityMatrix::getId)
            .containsExactlyInAnyOrder(with1.getId());

        assertThat(mskuAvailabilityMatrixRepository.find(
            new Filter().addWarehouseIds(1L, 2L)))
            .extracting(MskuAvailabilityMatrix::getId)
            .containsExactlyInAnyOrder(with1.getId(), with2.getId());

        //and with other conditions
        assertThat(mskuAvailabilityMatrixRepository.find(
            new Filter()
                .addWarehouseIds(1L, 0L)
                .setMskuId(with2.getMarketSkuId())))
            .isEmpty();
    }

    @Test(expected = DuplicateKeyException.class)
    public void shouldFail() {
        deepmindMskuRepository.save(randomMsku().setId(1L));
        mskuAvailabilityMatrixRepository.save(
            mskuAvailability(1L, 1L, "2003-01-01", "2003-12-31", false),
            mskuAvailability(1L, 1L, "2003-01-01", "2003-12-31", true)
        );
    }

    @Test(expected = DuplicateKeyException.class)
    public void shouldFailForRaw() {
        deepmindMskuRepository.save(randomMsku().setId(1L));
        mskuAvailabilityMatrixRepository.save(
            mskuAvailability(1L, 1L, null, null, false),
            mskuAvailability(1L, 1L, null, null, true)
        );
    }

    private MskuAvailabilityMatrix randomMatrix() {
        MskuAvailabilityMatrix result = random.nextObject(MskuAvailabilityMatrix.class, IGNORING_FIELDS)
            .setAvailable(true)
            .setMarketSkuId(msku.getId());

        if (result.getFromDate() != null
            && result.getToDate() != null
            && result.getFromDate().isAfter(result.getToDate())
        ) {
            LocalDate tmp = result.getFromDate();
            result.setFromDate(result.getToDate());
            result.setToDate(tmp);
        }
        return result;
    }

    private Msku randomMsku() {
        return random.nextObject(Msku.class)
            .setDeleted(false);
    }
}
