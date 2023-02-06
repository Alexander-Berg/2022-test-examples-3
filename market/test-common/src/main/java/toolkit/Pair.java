package toolkit;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pair<A, B> {
    private final A first;
    private final B second;

    public static <A, B> Pair<A, B> create(A first, B second) {
        return of(first, second);
    }

    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

}
