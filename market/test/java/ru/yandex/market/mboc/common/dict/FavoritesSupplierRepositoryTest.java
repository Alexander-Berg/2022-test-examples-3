package ru.yandex.market.mboc.common.dict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.favoritesupplier.FavoriteSupplier;
import ru.yandex.market.mboc.common.favoritesupplier.FavoriteSupplierRepository;
import ru.yandex.market.mboc.common.users.User;
import ru.yandex.market.mboc.common.users.UserRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author moskovkin@yandex-team.ru
 * @since 13.08.18
 */
public class FavoritesSupplierRepositoryTest extends BaseDbTestClass {
    private static final User TEST_USER_1 = new User("pablo");
    private static final User TEST_USER_2 = new User("hilberto");
    private static final Supplier TEST_SUPPLIER_1 = new Supplier(1, "MedellinSupplier");
    private static final Supplier TEST_SUPPLIER_2 = new Supplier(2, "KaliSupplier");

    private FavoriteSupplier favoriteS1U1;
    private FavoriteSupplier favoriteS1U2;
    private FavoriteSupplier favoriteS2U2;
    private FavoriteSupplier favoriteS2U1;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private FavoriteSupplierRepository favoriteSupplierRepository;

    @Before
    public void setup() {
        userRepository.insert(TEST_USER_1);
        userRepository.insert(TEST_USER_2);
        supplierRepository.insert(TEST_SUPPLIER_1);
        supplierRepository.insert(TEST_SUPPLIER_2);

        favoriteS1U1 = new FavoriteSupplier(TEST_SUPPLIER_1.getId(), TEST_USER_1.getId());
        favoriteS1U2 = new FavoriteSupplier(TEST_SUPPLIER_1.getId(), TEST_USER_2.getId());
        favoriteS2U2 = new FavoriteSupplier(TEST_SUPPLIER_2.getId(), TEST_USER_2.getId());
        favoriteS2U1 = new FavoriteSupplier(TEST_SUPPLIER_2.getId(), TEST_USER_1.getId());
    }

    @Test
    public void testInsert() {
        favoriteSupplierRepository.insert(favoriteS1U1);

        FavoriteSupplier dbFavorite = favoriteSupplierRepository.findById(favoriteS1U1);
        Assertions.assertThat(dbFavorite).isEqualToComparingFieldByFieldRecursively(favoriteS1U1);
    }

    @Test(expected = NoSuchElementException.class)
    public void testDelete() {
        favoriteSupplierRepository.insert(favoriteS1U1);

        FavoriteSupplier dbFavorite = favoriteSupplierRepository.findById(favoriteS1U1);
        Assertions.assertThat(dbFavorite).isEqualToComparingFieldByField(favoriteS1U1);

        favoriteSupplierRepository.delete(favoriteS1U1);
        favoriteSupplierRepository.findById(favoriteS1U1);
    }

    @Test
    public void testFindByMockUser() {
        favoriteSupplierRepository.insert(favoriteS1U1);
        favoriteSupplierRepository.insert(favoriteS1U2);
        favoriteSupplierRepository.insert(favoriteS2U2);

        List<FavoriteSupplier> favorites = favoriteSupplierRepository.findByMbocUser(TEST_USER_2.getId());

        Assertions.assertThat(favorites).containsExactlyInAnyOrder(
            favoriteS1U2,
            favoriteS2U2
        );
    }

    @Test
    public void testSupplierDeleteCascade() {
        favoriteSupplierRepository.insert(favoriteS1U2);
        favoriteSupplierRepository.insert(favoriteS2U2);

        supplierRepository.delete(TEST_SUPPLIER_1);

        List<FavoriteSupplier> favorites = favoriteSupplierRepository.findByMbocUser(TEST_USER_2.getId());
        Assertions.assertThat(favorites).containsExactlyInAnyOrder(
            favoriteS2U2
        );
    }

    @Test
    public void testFindByIds() {
        favoriteSupplierRepository.insert(favoriteS1U1);
        favoriteSupplierRepository.insert(favoriteS2U2);

        favoriteSupplierRepository.insert(favoriteS1U2);

        List<FavoriteSupplier> favorites = favoriteSupplierRepository
            .findByIds(Arrays.asList(favoriteS1U1, favoriteS2U2));

        Assertions.assertThat(favorites).containsExactlyInAnyOrder(
            favoriteS1U1,
            favoriteS2U2
        );
    }

    @Test
    public void testFindByIdsEmpty() {
        favoriteSupplierRepository.insert(favoriteS1U1);

        List<FavoriteSupplier> favorites = favoriteSupplierRepository
            .findByIds(new ArrayList<>());

        Assertions.assertThat(favorites).isEmpty();
    }

    @Test
    public void testDefaultOrderBy() {
        favoriteSupplierRepository.insert(favoriteS2U2);
        favoriteSupplierRepository.insert(favoriteS1U2);
        favoriteSupplierRepository.insert(favoriteS2U1);
        favoriteSupplierRepository.insert(favoriteS1U1);

        List<FavoriteSupplier> favorites = favoriteSupplierRepository.findByMbocUser(TEST_USER_1.getId());

        Assertions.assertThat(favorites).containsExactly(
            favoriteS1U1,
            favoriteS2U1
        );
    }
}
