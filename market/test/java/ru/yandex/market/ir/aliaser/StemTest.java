package ru.yandex.market.ir.aliaser;

import junit.framework.TestCase;
import org.springframework.util.StopWatch;
import org.tartarus.snowball.ext.russianStemmer;

public class StemTest extends TestCase {

    private static ThreadLocal<russianStemmer> stemmer = new ThreadLocal<russianStemmer>() {
        @Override
        protected russianStemmer initialValue() {
            return new russianStemmer();
        }
    };

    public static String stem(String s) {
        russianStemmer p = stemmer.get();
        p.setCurrent(s);
        p.stem();
        return p.getCurrent();

    }

    public void testStem() {
        assertEquals(stem("камня"), stem("камней"));
        StopWatch watch = new StopWatch();
        watch.start();
        for (int i = 0; i < 1000000; i++) {
            stem("камней");
        }
        watch.stop();
        System.out.println(watch.prettyPrint());
    }

}
