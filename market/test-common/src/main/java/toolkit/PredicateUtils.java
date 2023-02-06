package toolkit;


import java.util.function.Consumer;
import java.util.function.Predicate;

public class PredicateUtils {
    private PredicateUtils() {
    }

    public static <T> Predicate<T> loggingPredicate(Predicate<T> predicate, Consumer<T> reporter) {
        return o -> {
            boolean result = predicate.test(o);
            if (!result) {
                reporter.accept(o);
            }
            return result;
        };
    }
}
