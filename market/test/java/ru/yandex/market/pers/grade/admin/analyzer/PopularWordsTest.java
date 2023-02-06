package ru.yandex.market.pers.grade.admin.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.russianStemmer;

import ru.yandex.market.pers.grade.admin.MockedPersGradeAdminTest;

/**
 * @author imelnikov
 */
public class PopularWordsTest extends MockedPersGradeAdminTest {
    private static final Logger log = Logger.getLogger(PopularWordsTest.class);

    @Test
    public void testGetPopularWords() throws Exception {
        log.setLevel(Level.INFO);
        prepareExclude();
        for (int shopId : shops) {
            countWords(shopId);
        }
    }

    private static final int SIZE_COMMON = 150;
    private static final int SIZE_POPULAR = 100;

    int[] shops = {193, 704, 720, 829, 1497, 1550, 1613, 1672, 1925, 2117, 3458, 3828, 4106, 4398, 4624, 4779, 4932, 7781, 17019, 17436, 18063, 18433, 19790, 22192, 22504, 30746, 31073, 43150, 58825};

    private SnowballStemmer snowballStemmer = new russianStemmer();

    //language=SQL
    private String loadAllQuery =
        "select short_text " +
            "from grade g " +
            "join ( " +
            "  select resource_id " +
            "  from grade " +
            "  where type=0 and state=0 " +
            "  and cr_time > now() - interval '90' day " +
            "  group by resource_id " +
            "  having count(*) > 50 " +
            ") s on g.resource_id=s.resource_id " +
            "where type=0 and state=0 and short_text is not null " +
            "and gr_0 = 2 " +
            "and cr_time > now() - interval '90' day ";

    //language=SQL
    private String loadShopQuery =
        "select short_text " +
            "from grade g " +
            "where type=0 and state=0 and short_text is not null " +
            "and gr_0 = 2 " +
            "and cr_time > now() - interval '90' day " +
            "and resource_id = ";


    private int total = 0;
    private Map<String, Integer> words = new HashMap<>();
    private Map<String, Integer> exclude;

    public void prepareExclude() {
        loadWords(loadAllQuery);
        exclude = popular(SIZE_COMMON);
        log.debug("Exclude: " + exclude.size());
    }

    public void countWords(int shopId) throws Exception {
        int grades = loadWords(loadShopQuery + shopId);
        log.debug(grades + " grades processed");
        Map<String, Integer> popular = popular(SIZE_POPULAR, exclude);

        int num = popular.size();
        //		log.info("Popular "+num);
        // process popular words
        int sum = 0;
        int max = 0;
        for (Entry<String, Integer> e : sort(popular)) {
            Integer count = e.getValue();
            sum += count;
            if (count > max) {
                max = count;
            }
        }

        System.out.println(shopId + "\t" + grades + "\t" + num + "\t" + sum + "\t" + (num == 0 ? 0 : sum / num) + "\t" + max);
    }

    private int loadWords(String sql) {
        log.debug("sql: \n" + sql);
        total = 0;
        RowCallbackHandler handler = rs -> {
            String text = rs.getString(1);
            for (String word : getWords(text)) {
                processWord(word);
            }
            total++;
        };
        pgJdbcTemplate.query(sql, handler);
        return total;
    }

    /**
     * Extract stems from grade
     */
    private List<String> getWords(String text) {
        text = text.toLowerCase().replaceAll("[^а-я]", " ").replaceAll("[ ]+", " ");
        ArrayList<String> result = new ArrayList<>();
        for (String word : text.split("\\s")) {
            snowballStemmer.setCurrent(word);
            snowballStemmer.stem();
            String stem = snowballStemmer.getCurrent();
            if (stem.length() > 2) {
                result.add(stem);
            }
        }
        return result;
    }

    private void processWord(String word) {
        Integer count = words.get(word);
        if (count == null) {
            count = 1;
        } else {
            count += 1;
        }
        words.put(word, count);
    }

    private Map<String, Integer> popular(int size) {
        return popular(size, Collections.EMPTY_MAP);
    }

    private Map<String, Integer> popular(int size, Map<String, Integer> exclude) {
        Map<String, Integer> result = new HashMap<>();

        for (Entry<String, Integer> e : sort(words)) {
            if (!exclude.containsKey(e.getKey())) {
                result.put(e.getKey(), e.getValue());
                if (result.size() == size) {
                    break;
                }
            }
        }

        return result;
    }


    private static ArrayList<Entry<String, Integer>> sort(Map<String, Integer> words) {
        ArrayList<Entry<String, Integer>> list = new ArrayList<>(words.entrySet());
        Collections.sort(list, (o1, o2) -> o2.getValue() - o1.getValue());
        return list;
    }
}
