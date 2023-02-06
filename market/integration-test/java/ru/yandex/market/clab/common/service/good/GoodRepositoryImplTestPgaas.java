package ru.yandex.market.clab.common.service.good;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.ControlledClock;
import ru.yandex.market.clab.common.service.ConcurrentModificationException;
import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.Sorting;
import ru.yandex.market.clab.common.service.user.User;
import ru.yandex.market.clab.common.service.user.UserRepository;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.ClabUser;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.clab.db.jooq.generated.Tables.GOOD;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 11.10.2018
 */
public class GoodRepositoryImplTestPgaas extends BasePgaasIntegrationTest {
    private static final String[] CHECK_IGNORE_FIELDS = new String[] {
        "id",
        "modifiedDate",
        "lastChangeDate"
    };
    private static final String[] CHECK_IGNORE_FIELDS_NO_DATA = new String[] {
        "id",
        "modifiedDate",
        "lastChangeDate",
        "storageMsku",
        "editedMsku",
        "storageModification",
        "storageModel",
        "editedModel",
        "editedModification",
        "baseMsku",
        "baseModel",
        "baseModification",
        "baseLastReadMsku",
        "baseLastReadModel",
        "baseLastReadModification"
    };

    @Autowired
    private GoodRepository goodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ControlledClock clock;

    @Test
    public void simpleInsert() {
        Good good = createGood();
        good.setCartId(42L);
        good.setWhBarcode("barcode");

        Good saved = goodRepository.save(good);
        assertThat(good.getId()).withFailMessage("should not affect original object").isNull();
        assertThat(good.getModifiedDate()).withFailMessage("should not affect original object").isNull();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCartId()).isEqualTo(good.getCartId());
        assertThat(saved.getWhBarcode()).isEqualTo(good.getWhBarcode());
        assertThat(saved.getModifiedDate()).isNotNull();
    }

    @Test
    public void batchInsert() {
        List<Good> goods = ImmutableList.of(
            createGood(),
            createGood(),
            createGood());

        List<Good> saved = goodRepository.save(goods, ActionSource.USER);

        assertThat(saved)
            .usingElementComparatorIgnoringFields(CHECK_IGNORE_FIELDS)
            .containsExactlyElementsOf(goods);
    }

    @Test
    public void simpleUpdate() {
        Good good = createGood();
        good.setWhBarcode("barcode-1");

        Good saved = goodRepository.save(good);
        long id = saved.getId();
        assertThat(saved.getWhBarcode()).isEqualTo("barcode-1");

        saved.setWhBarcode("barcode-2");
        clock.tickMinute();
        Good updated = goodRepository.save(saved);
        assertThat(updated.getId()).isEqualTo(id);
        assertThat(updated.getModifiedDate()).isAfter(saved.getModifiedDate());

        Good fetched = goodRepository.getById(id);
        assertThat(fetched.getWhBarcode()).isEqualTo("barcode-2");
        assertThat(fetched.getModifiedDate()).isEqualTo(updated.getModifiedDate());
    }

    @Test
    public void batchUpdateUpdatesModifiedDate() {
        Good saved = goodRepository.save(createGood());
        saved.setWhBarcode("new-barcode");

        clock.tickMinute();
        goodRepository.save(Collections.singleton(saved));

        Good fetched = goodRepository.getById(saved.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getModifiedDate()).isAfter(saved.getModifiedDate());
    }

    @Test
    public void optimisticLocking() {
        Good saved = goodRepository.save(createGood());

        goodRepository.save(saved);

        assertThatThrownBy(() -> {

            saved.setCartId(15L);
            goodRepository.save(saved);

        }).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void optimisticLockBatchUpdate() {
        Good good1 = goodRepository.save(createGood().setWhBarcode("barcode1"));
        Good good2 = goodRepository.save(createGood().setWhBarcode("barcode2"));
        goodRepository.save(good1);

        good1.setWhBarcode("newbarcode1");
        good2.setWhBarcode("newbarcode2");

        assertThatThrownBy(() -> {
            goodRepository.save(Arrays.asList(good1, good2));
        }).isInstanceOf(ConcurrentModificationException.class);

        Good actualGood1 = goodRepository.getById(good1.getId());
        Good actualGood2 = goodRepository.getById(good2.getId());

        assertThat(actualGood1).isNotNull();
        assertThat(actualGood2).isNotNull();
        assertThat(actualGood1.getWhBarcode()).isEqualTo("barcode1");
        // Unfortunaetly spring test creates transaction on test method and rollbacks it after exit from tst method.
        // We can't create nested transaction, so we can only assert on state inside the transaction.
        assertThat(actualGood2.getWhBarcode()).isEqualTo("newbarcode2");
    }

    @Test
    public void findNoData() {
        assertThat(GoodRepositoryImpl.NO_DATA_FIELDS)
            .isNotEmpty()
            .doesNotContain(GOOD.EDITED_MSKU);

        Good good = goodRepository.save(
            createGood()
            .setBaseLastReadModel(RandomTestUtils.randomBytes())
            .setEditedModel(RandomTestUtils.randomBytes())
            .setStorageMsku(RandomTestUtils.randomBytes())
        );

        List<Good> goods = goodRepository.findNoData(GoodFilter.any(), Sorting.defaultSorting(), PageFilter.all());
        assertThat(goods).hasSize(1);
        Good fetched = goods.get(0);

        assertThat(fetched.getId()).isEqualTo(good.getId());
        assertThat(fetched.getBaseLastReadModel()).isNull();
        assertThat(fetched.getEditedModel()).isNull();
        assertThat(fetched.getStorageMsku()).isNull();
    }

    @Test
    public void sortByEditor() {
        User user1 = createAndSaveUser("aaa");
        User user2 = createAndSaveUser("bbb");
        User user3 = createAndSaveUser("ccc");

        Good good1 = goodRepository.save(createGood().setEditorId(user1.getId()));
        Good good2 = goodRepository.save(createGood().setEditorId(user2.getId()));
        Good good3 = goodRepository.save(createGood().setEditorId(user3.getId()));

        List<Good> goods = goodRepository.findNoData(
            GoodFilter.any(), Sorting.asc(GoodSortBy.EDITOR), PageFilter.all(), user2
        );
        assertThat(goods).usingElementComparatorIgnoringFields(CHECK_IGNORE_FIELDS_NO_DATA)
            .containsExactly(good2, good1, good3);
    }

    @Test
    public void sortByPhotoEditor() {
        User user1 = createAndSaveUser("aaa");
        User user2 = createAndSaveUser("bbb");
        User user3 = createAndSaveUser("ccc");

        Good good1 = goodRepository.save(createGood().setPhotoEditorId(user1.getId()));
        Good good2 = goodRepository.save(createGood().setPhotoEditorId(user2.getId()));
        Good good3 = goodRepository.save(createGood().setPhotoEditorId(user3.getId()));

        List<Good> goods = goodRepository.findNoData(
            GoodFilter.any(), Sorting.asc(GoodSortBy.PHOTO_EDITOR), PageFilter.all(), user2
        );
        assertThat(goods)
            .usingElementComparatorIgnoringFields(CHECK_IGNORE_FIELDS_NO_DATA)
            .containsExactly(good2, good1, good3);
    }

    private Good createGood() {
        return RandomTestUtils.randomObject(Good.class, "id", "modifiedDate");
    }

    private User createAndSaveUser(String login) {
        ClabUser result = userRepository.save(new ClabUser()
            .setLogin(login)
            .setRoles("ADMIN"));
        User user = new User(result.getLogin());
        user.setId(result.getId());
        return user;
    }
}
