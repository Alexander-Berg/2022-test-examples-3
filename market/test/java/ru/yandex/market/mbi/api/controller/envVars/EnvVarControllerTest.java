package ru.yandex.market.mbi.api.controller.envVars;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.RestTemplateFactory;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.api.controller.envvars.EnvVarController;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Функциональные тесты на {@link EnvVarController}.
 *
 * @author romger
 */
@DbUnitDataSet(before = "EnvVarsTest.csv")
public class EnvVarControllerTest extends FunctionalTest {
    private static final long TOTAL_COUNT = 11;
    private static final int PAGE = 0;
    private static final int PAGE_SIZE = 3;

    /**
     * Тест для ручки {@code /env-vars/list}.
     * Проверка получения информации о environments с параметрами по-умолчанию
     */
    @Test
    void testGetEnvVarsList() {
        RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
        String response = restTemplate.getForObject(
                "http://localhost:" + port + "/env-vars/list?page=" + PAGE + "&pageSize=" + PAGE_SIZE,
                String.class);
        MbiAsserts.assertXmlEquals("<env-vars-collection>\n" +
                "    <items>\n" +
                "        <item>\n" +
                "            <name>a</name>\n" +
                "            <values>\n" +
                "                <values>10</values>\n" +
                "                <values>4</values>\n" +
                "                <values>5</values>\n" +
                "                <values>9</values>\n" +
                "            </values>\n" +
                "            <total-values>4</total-values>\n" +
                "        </item>\n" +
                "        <item>\n" +
                "            <name>b</name>\n" +
                "            <values>\n" +
                "                <values>5</values>\n" +
                "            </values>\n" +
                "            <total-values>1</total-values>\n" +
                "        </item>\n" +
                "        <item>\n" +
                "            <name>c</name>\n" +
                "            <values>\n" +
                "                <values>0</values>\n" +
                "            </values>\n" +
                "            <total-values>1</total-values>\n" +
                "        </item>\n" +
                "    </items>\n" +
                "    <total-count>" + TOTAL_COUNT + "</total-count>\n" +
                "</env-vars-collection>",
                response);
    }

    /**
     * Тест для ручки {@code /env-vars/list}.
     * Проверка получения информации о environments с сортировкой по полю
     */
    @Test
    void testGetEnvVarsListWithSort() {
        RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
        String response = restTemplate.getForObject(
                "http://localhost:" + port + "/env-vars/list?page=" + PAGE + "&pageSize=" + PAGE_SIZE + "&sortType=DESC",
                String.class);
        MbiAsserts.assertXmlEquals("<env-vars-collection>\n" +
                        "    <items>\n" +
                        "        <item>\n" +
                        "            <name>weer</name>\n" +
                        "            <values>\n" +
                        "                <values>1</values>\n" +
                        "                <values>10</values>\n" +
                        "                <values>11</values>\n" +
                        "                <values>12</values>\n" +
                        "                <values>13</values>\n" +
                        "                <values>14</values>\n" +
                        "                <values>15</values>\n" +
                        "                <values>16</values>\n" +
                        "                <values>17</values>\n" +
                        "                <values>18</values>\n" +
                        "                <values>19</values>\n" +
                        "                <values>2</values>\n" +
                        "                <values>20</values>\n" +
                        "                <values>21</values>\n" +
                        "                <values>22</values>\n" +
                        "                <values>23</values>\n" +
                        "                <values>24</values>\n" +
                        "                <values>25</values>\n" +
                        "                <values>3</values>\n" +
                        "                <values>4</values>\n" +
                        "            </values>\n" +
                        "            <total-values>25</total-values>\n" +
                        "        </item>\n" +
                        "        <item>\n" +
                        "            <name>j</name>\n" +
                        "            <values>\n" +
                        "                <values>9</values>\n" +
                        "            </values>\n" +
                        "            <total-values>1</total-values>\n" +
                        "        </item>\n" +
                        "        <item>\n" +
                        "            <name>i</name>\n" +
                        "            <values>\n" +
                        "                <values>3</values>\n" +
                        "            </values>\n" +
                        "            <total-values>1</total-values>\n" +
                        "        </item>\n" +
                        "    </items>\n" +
                        "    <total-count>" + TOTAL_COUNT + "</total-count>\n" +
                        "</env-vars-collection>",
                response);
    }

    /**
     * Тест для ручки {@code /env-vars/list}.
     * Проверка получения информации о environments с пейджинацией
     */
    @Test
    void testGetEnvVarsListWithPage() {
        final int page = 2;
        RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
        String response = restTemplate.getForObject(
                "http://localhost:" + port + "/env-vars/list?page=" + page + "&pageSize=" + PAGE_SIZE,
                String.class);
        MbiAsserts.assertXmlEquals("<env-vars-collection>\n" +
                        "    <items>\n" +
                        "        <item>\n" +
                        "            <name>g</name>\n" +
                        "            <values>\n" +
                        "                <values>2</values>\n" +
                        "            </values>\n" +
                        "            <total-values>1</total-values>\n" +
                        "        </item>\n" +
                        "        <item>\n" +
                        "            <name>h</name>\n" +
                        "            <values>\n" +
                        "                <values>8</values>\n" +
                        "            </values>\n" +
                        "            <total-values>1</total-values>\n" +
                        "        </item>\n" +
                        "        <item>\n" +
                        "            <name>i</name>\n" +
                        "            <values>\n" +
                        "                <values>3</values>\n" +
                        "            </values>\n" +
                        "            <total-values>1</total-values>\n" +
                        "        </item>\n" +
                        "    </items>\n" +
                        "    <total-count>" + TOTAL_COUNT + "</total-count>\n" +
                        "</env-vars-collection>",
                response);
    }

    /**
     * Тест для ручки {@code /env-vars/list}.
     * Проверка получения информации о environments с фильтрацией по name
     */
    @Test
    void testGetEnvVarsListWithFilterByName() {
        RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
        String response = restTemplate.getForObject(
                "http://localhost:" + port + "/env-vars/list?page=" + PAGE + "&pageSize=" + PAGE_SIZE + "&name=dfg",
                String.class);
        MbiAsserts.assertXmlEquals("<env-vars-collection>\n" +
                        "    <items>\n" +
                        "        <item>\n" +
                        "            <name>edFgh</name>\n" +
                        "            <values>\n" +
                        "                <values>1</values>\n" +
                        "            </values>\n" +
                        "            <total-values>1</total-values>\n" +
                        "        </item>\n" +
                        "    </items>\n" +
                        "    <total-count>1</total-count>\n" +
                        "</env-vars-collection>",
                response);
    }

    /**
     * Тест для ручки {@code /env-vars/card}.
     * Проверка получения полной информации о переменной по её имени
     */
    @Test
    void testGetEnvVarCardByName() {
        RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
        String response = restTemplate.getForObject(
                "http://localhost:" + port + "/env-vars/card?name=weer",
                String.class);
        MbiAsserts.assertXmlEquals("<env-var>\n" +
                        "            <name>weer</name>\n" +
                        "            <values>\n" +
                        "                <values>1</values>\n" +
                        "                <values>10</values>\n" +
                        "                <values>11</values>\n" +
                        "                <values>12</values>\n" +
                        "                <values>13</values>\n" +
                        "                <values>14</values>\n" +
                        "                <values>15</values>\n" +
                        "                <values>16</values>\n" +
                        "                <values>17</values>\n" +
                        "                <values>18</values>\n" +
                        "                <values>19</values>\n" +
                        "                <values>2</values>\n" +
                        "                <values>20</values>\n" +
                        "                <values>21</values>\n" +
                        "                <values>22</values>\n" +
                        "                <values>23</values>\n" +
                        "                <values>24</values>\n" +
                        "                <values>25</values>\n" +
                        "                <values>3</values>\n" +
                        "                <values>4</values>\n" +
                        "                <values>5</values>\n" +
                        "                <values>6</values>\n" +
                        "                <values>7</values>\n" +
                        "                <values>8</values>\n" +
                        "                <values>9</values>\n" +
                        "            </values>\n" +
                        "            <total-values>25</total-values>\n" +
                        "        </env-var>",
                response);
    }

    /**
     * Тест для ручки {@code /env-vars/card}.
     * Проверка получения нулевого ответа при запросе несуществующей карточки
     */
    @Test
    void testGetNullEnvVarCardByName() {
        RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
        assertThrows(HttpClientErrorException.NotFound.class, () -> restTemplate.getForObject(
                "http://localhost:" + port + "/env-vars/card?name=wee",
                String.class));
    }
}
