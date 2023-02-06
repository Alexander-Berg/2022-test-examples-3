package ru.yandex.calendar.logic.resource;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.bolts.function.forhuman.Comparator;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class SubjectIdComparatorTest {

    private static <A> Tuple2List<A, A> allPairs(ListF<A> list) {
        Tuple2List<A, A> r = Cf.Tuple2List.arrayList();
        for (A a1 : list) {
            for (A a2 : list) {
                r.add(a1, a2);
            }
        }
        return r;
    }

    @Test
    public void test() {
        UidOrResourceId r1 = UidOrResourceId.resource(123456);
        UidOrResourceId r2 = UidOrResourceId.resource(456789);
        UidOrResourceId u1 = UidOrResourceId.user(PassportUid.cons(123456));
        UidOrResourceId u2 = UidOrResourceId.user(PassportUid.cons(456789));

        Tuple2List<UidOrResourceId, Integer> list = Cf.list(r1, r2, u1, u2).zipWithIndex();
        Tuple2List<Tuple2<UidOrResourceId, Integer>, Tuple2<UidOrResourceId, Integer>> allPairs = allPairs(list);

        Comparator<UidOrResourceId> c = SubjectIdComparator.comparator();

        for (Tuple2<Tuple2<UidOrResourceId, Integer>, Tuple2<UidOrResourceId, Integer>> p : allPairs) {
            Assert.A.equals(
                Integer.signum(c.compare(p._1._1, p._2._1)),
                Integer.signum(Cf.Integer.comparator().compare(p._1._2, p._2._2))
            );
        }
    }

} //~
