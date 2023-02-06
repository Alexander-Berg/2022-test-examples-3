package ru.yandex.autotests.innerpochta.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 09.06.15
 * Time: 17:28
 */
public class FirstAndCountData {

    public static List<Object[]> get(int pinned, int notPinned) {
        List<Object[]> operList = new ArrayList<Object[]>();
        //mailbox_list
        operList.add(new Object[]{0, 0});

        operList.add(new Object[]{0, pinned - 1});
        operList.add(new Object[]{0, pinned});
        operList.add(new Object[]{0, pinned + 1});

        operList.add(new Object[]{pinned - 1, pinned + 1});
        operList.add(new Object[]{pinned - 1, pinned + notPinned});

        operList.add(new Object[]{pinned, pinned + notPinned});
        operList.add(new Object[]{0, pinned + notPinned});
        operList.add(new Object[]{0, pinned + notPinned + 100});

        operList.add(new Object[]{pinned + notPinned, 0});
        operList.add(new Object[]{pinned + notPinned,
                pinned + notPinned + 10});
        return operList;
    }
}

