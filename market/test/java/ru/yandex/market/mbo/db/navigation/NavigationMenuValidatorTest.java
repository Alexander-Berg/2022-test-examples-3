package ru.yandex.market.mbo.db.navigation;

import org.junit.Test;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.gwt.models.navigation.Block;
import ru.yandex.market.mbo.gwt.models.navigation.Departament;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationMenu;

import java.util.Collections;

/**
 * @author galaev
 * @since 2019-02-27
 */
public class NavigationMenuValidatorTest {

    private NavigationMenuValidator validator = new NavigationMenuValidator();

    @Test
    public void validMenuTest() {
        NavigationMenu menu = createValidMenu();
        validator.validateNavigationMenu(menu);
    }

    @Test(expected = OperationException.class)
    public void menuWithoutDepartmentsTest() {
        NavigationMenu menu = createMenu();
        validator.validateNavigationMenu(menu);
    }

    @Test(expected = OperationException.class)
    public void menuWithNullBlockTest() {
        NavigationMenu menu = createMenu();
        Departament departament = new Departament();
        menu.setDepartaments(Collections.singletonList(departament));

        validator.validateNavigationMenu(menu);
    }

    @Test(expected = OperationException.class)
    public void menuWithoutChildBlocksTest() {
        NavigationMenu menu = createMenu();
        Departament departament = new Departament();
        Block block = new Block();
        departament.setBlock(block);
        menu.setDepartaments(Collections.singletonList(departament));

        validator.validateNavigationMenu(menu);
    }

    @Test(expected = OperationException.class)
    public void menuWithNullNavigationNodeIdTest() {
        NavigationMenu menu = createMenu();
        Departament departament = new Departament();
        Block block = new Block();
        Block childBlock = new Block();
        block.setBlocks(Collections.singletonList(childBlock));
        departament.setBlock(block);
        menu.setDepartaments(Collections.singletonList(departament));

        validator.validateNavigationMenu(menu);
    }

    private NavigationMenu createValidMenu() {
        NavigationMenu menu = createMenu();
        Departament departament = new Departament();
        Block block = new Block();
        block.setNavigationNodeId(1L);
        Block childBlock = new Block();
        childBlock.setNavigationNodeId(2L);
        block.setBlocks(Collections.singletonList(childBlock));
        departament.setBlock(block);
        menu.setDepartaments(Collections.singletonList(departament));
        return menu;
    }

    private NavigationMenu createMenu() {
        NavigationMenu menu = new NavigationMenu();
        menu.setId(0);
        menu.setName("");
        menu.setDepartaments(Collections.emptyList());
        return menu;
    }
}
