package ru.yandex.market.mbo.db.recipes;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.recipe.ImportRecipesResult;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeSqlFilter;
import ru.yandex.market.mbo.recipe.csvimport.RecipeCSVImporter;
import ru.yandex.market.mbo.recipe.url.RecipeFromUrlBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.market.mbo.db.recipes.RecipeValidatorTest.getPublishedCategory;

/**
 * @author padme
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class RecipeCSVImporterTest {

    private RecipeCSVImporter csvImporter;
    private final Long autoUser = 28027378L;

    private final String startPath = "/recipes/examples/";

    private RecipeService recipeService;

    @Before
    public void setUp() throws Exception {
        RecipeFromUrlBuilder recipeUrlParser = new RecipeFromUrlBuilder(new RecipeFromUrlHelperMock());
        recipeService = new RecipeService(null, new RecipeServiceDaoMock());

        csvImporter = new RecipeCSVImporter(autoUser,
            recipeService,
            recipeUrlParser,
            new TovarTreeServiceMock(getPublishedCategory()));
    }

    @Test
    public void importCorrectCSVFile() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "correct.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(0, result.getBadLines().size());
            assertEquals(3, result.getCreateCount());
            assertEquals(1, result.getDeleteCount());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void importRecipeWithoutCategoryId() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "notCorrect1.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(1, result.getBadLines().size());
            assertEquals("Ошибка парсинга. Неверный формат url лендинга. Not found parameter 'hid' in url",
                result.getBadLines().get(0).getErrorMessage());

            Recipe recipe = result.getBadLines().get(0).getRecipe();
            assertEquals("Chanel", recipe.getName());
            assertEquals("Блеск для губ Chanel", recipe.getHeader());
            assertEquals(3359, recipe.getPopularity());
            assertEquals(0, recipe.getId());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void importRecipeWithWrongUrl() throws Exception {
        /*
         * correct filter start with glfilter
         * gfilter, filter is wrong form!
         */
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "notCorrect2.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(1, result.getBadLines().size());
            assertEquals("Нет ни одного фильтра. ",
                result.getBadLines().get(0).getErrorMessage());

            Recipe recipe = result.getBadLines().get(0).getRecipe();
            assertEquals("Bourjois", recipe.getName());
            assertEquals("Блеск для губ Bourjois", recipe.getHeader());
            assertEquals(3113, recipe.getPopularity());
            assertEquals(0, recipe.getId());
            assertEquals(0, recipe.getFilters().size());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void importRecipeWithWrongParams() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "notCorrect3.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(1, result.getBadLines().size());
            assertEquals("Ошибка парсинга. Неверный формат url лендинга. Parameter 321 not found",
                result.getBadLines().get(0).getErrorMessage());

            Recipe recipe = result.getBadLines().get(0).getRecipe();
            assertEquals("Guerlain", recipe.getName());
            assertEquals("Блеск для губ Guerlain", recipe.getHeader());
            assertEquals(2076, recipe.getPopularity());
            assertEquals(2, recipe.getId());
            assertEquals(0, recipe.getFilters().size());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void importWithNotExistHid() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "notCorrect4.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(1, result.getBadLines().size());
            assertEquals("Не существует категории с hid 1234",
                result.getBadLines().get(0).getErrorMessage());

            Recipe recipe = result.getBadLines().get(0).getRecipe();
            assertEquals("Guerlain", recipe.getName());
            assertEquals("Блеск для губ Guerlain", recipe.getHeader());
            assertEquals(2076, recipe.getPopularity());
            assertEquals(2, recipe.getId());
            assertEquals(1, recipe.getFilters().size());

            RecipeFilter filter = recipe.getFilters().get(0);
            assertEquals(30, filter.getParamId().longValue());
            assertEquals(Param.Type.NUMERIC, filter.getParamType());
            assertEquals(new BigDecimal(10), filter.getMinValue());
            assertEquals(new BigDecimal(20), filter.getMaxValue());

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void importRecipeWithTheSameName() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "notCorrect5.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(1, result.getBadLines().size());
            assertEquals("Короткое название должно быть уникальным в рамках категории 1: Name_1",
                result.getBadLines().get(0).getErrorMessage());

            Recipe recipe = result.getBadLines().get(0).getRecipe();
            assertEquals("Name_1", recipe.getName());
            assertEquals("Name_1", recipe.getHeader());
            assertEquals(2076, recipe.getPopularity());
            assertEquals(20, recipe.getId());
            assertEquals(1, recipe.getFilters().size());

            RecipeFilter filter = recipe.getFilters().get(0);
            assertEquals(30, filter.getParamId().longValue());
            assertEquals(Param.Type.NUMERIC, filter.getParamType());
            assertEquals(new BigDecimal(10), filter.getMinValue());
            assertEquals(new BigDecimal(20), filter.getMaxValue());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void importRecipeWithTheSameHeader() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "notCorrect6.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(1, result.getBadLines().size());
            assertEquals("Заголовок лендинга должен быть уникальным среди опубликованных: Header_1",
                result.getBadLines().get(0).getErrorMessage());

            Recipe recipe = result.getBadLines().get(0).getRecipe();
            assertEquals("Header_1", recipe.getName());
            assertEquals("Header_1", recipe.getHeader());
            assertEquals(2076, recipe.getPopularity());
            assertEquals(20, recipe.getId());
            assertEquals(1, recipe.getFilters().size());

            RecipeFilter filter = recipe.getFilters().get(0);
            assertEquals(30, filter.getParamId().longValue());
            assertEquals(Param.Type.NUMERIC, filter.getParamType());
            assertEquals(new BigDecimal(10), filter.getMinValue());
            assertEquals(new BigDecimal(20), filter.getMaxValue());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /*
     * Check new version of recipe loader file.
     * with information about market buttons
     */
    @Test
    public void importCorrectCSVFileV2() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "correct2.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(0, result.getBadLines().size());
            assertEquals(5, result.getCreateCount());
            assertEquals(0, result.getDeleteCount());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void importNotCorrectCSVFileV2() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "notCorrectV2.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(1, result.getBadLines().size());
            assertEquals(4, result.getCreateCount());
            assertEquals(0, result.getDeleteCount());

            Recipe recipe = result.getBadLines().get(0).getRecipe();
            assertEquals("С защитой от протечек", recipe.getName());
            assertEquals("Посудомоечные машины с защитой от протечек", recipe.getHeader());
            assertEquals(1009803, recipe.getPopularity());
            assertEquals(0, recipe.getId());
            assertEquals(1, recipe.getFilters().size());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void importCorrectCSVFile3() throws Exception {
        // Импортируем рецепт с названием как у навигационного рецепта.
        // Импортируем рецепт с заголовком как у навигационного рецепта.
        // Импорт должен проходить без ошибок
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "correct3.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(0, result.getBadLines().size());
            assertEquals(2, result.getCreateCount());
            assertEquals(0, result.getDeleteCount());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /*
     * Check new version of recipe loader file.
     * with information about market buttons
     */
    @Test
    public void importCorrectDiscount() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "correctDiscount.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(1, result.getBadLines().size());
            assertEquals("Только одна галка 'Скидки' или 'Скидки и акции' может быть равна 1",
                result.getBadLines().get(0).getErrorMessage());
            assertEquals(3, result.getCreateCount());
            assertEquals(0, result.getDeleteCount());

            Recipe recipe = result.getBadLines().get(0).getRecipe();
            assertEquals("Трещотки", recipe.getName());
            assertEquals("Трещотки", recipe.getHeader());
            assertEquals(11, recipe.getPopularity());
            assertEquals(0, recipe.getId());
            assertEquals(1, recipe.getFilters().size());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void checkValidation() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "checkValidation1.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(6, result.getBadLines().size());
            assertEquals(1, result.getCreateCount());
            assertEquals(0, result.getDeleteCount());

            assertEquals(result.getBadLines().get(0).getErrorMessage(),
                "Поле 'Заголовок лендинга' должно быть заполнено. ");
            assertEquals(result.getBadLines().get(1).getErrorMessage(),
                "Либо поле 'Короткое название', либо 'Текст кнопки' должно быть заполнено. ");
            assertEquals(result.getBadLines().get(2).getErrorMessage(),
                "Либо поле 'Индекс для ранжирования кнопок', " +
                    "либо 'Количество запросов в год' должно быть заполнено. ");
            assertEquals(result.getBadLines().get(3).getErrorMessage(),
                "Поле 'Короткое название' должно быть заполнено. ");
            assertEquals(result.getBadLines().get(4).getErrorMessage(),
                "Поле 'Заголовок лендинга' должно быть заполнено. ");
            assertEquals(result.getBadLines().get(5).getErrorMessage(),
                "Количество запросов должно быть не отрицательным числом. ");

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /*
     * Check space in the end of name of title of recipe
     */
    @Test
    public void checkSpace() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "space.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(0, result.getBadLines().size());
            assertEquals(2, result.getCreateCount());
            assertEquals(0, result.getDeleteCount());

            Recipe spaceRecipe = getSearchRecipes(1L).get(0);
            assertEquals("space", spaceRecipe.getHeader());
            assertEquals("space", spaceRecipe.getName());

            Recipe noSpaceRecipe = getSearchRecipes(2L).get(0);
            assertEquals("no space", noSpaceRecipe.getHeader());
            assertEquals("no space", noSpaceRecipe.getName());

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /*
     * Check recipes without filters
     */
    @Test
    public void checkWithoutFilters() throws Exception {
        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "withoutFilters.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);
            assertEquals(2, result.getBadLines().size());
            assertEquals(result.getBadLines().get(0).getErrorMessage(),
                "Нет ни одного фильтра. ");
            assertEquals(result.getBadLines().get(1).getErrorMessage(),
                "Поисковый запрос обязателен для заполнения для рецептов без фильтров. ");
            assertEquals(1, result.getCreateCount());
            assertEquals(0, result.getDeleteCount());

            Recipe recipe = result.getBadLines().get(0).getRecipe();
            assertEquals("MBO-16644-2", recipe.getName());
            assertEquals("MBO-16644-2", recipe.getHeader());
            assertEquals(10, recipe.getPopularity());
            assertEquals(0, recipe.getId());
            assertEquals(0, recipe.getFilters().size());

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    @Issue("MBO-19987")
    public void checkAutoGenerated() throws Exception {
        // Новые рецепты из csv должены импортироваться с флагом auto_generated=False
        // При обновлени рецептов из csv auto_generated должен проставляться в False
        Recipe autoRecipe = getSearchRecipes(8L).get(0);
        assertTrue(autoRecipe.isAutoGenerated());

        try (InputStream io = RecipeCSVImporterTest.class.getResourceAsStream(startPath + "autoGenerated.csv")) {
            ImportRecipesResult result = csvImporter.importRecipes(io, autoUser);

            List<Recipe> recipes = getSearchRecipes();
            for (Recipe recipe : recipes) {
                assertFalse(recipe.isAutoGenerated());
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private List<Recipe> getSearchRecipes(Long... ids) {
        RecipeSqlFilter filter = new RecipeSqlFilter();
        filter.setIds(ids);
        return recipeService.getSearchRecipes(filter, -1, -1,
            RecipeSqlFilter.Field.ID, true, true);
    }
}
