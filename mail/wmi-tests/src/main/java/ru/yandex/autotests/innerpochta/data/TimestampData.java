package ru.yandex.autotests.innerpochta.data;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 19.03.14
 * Time: 16:36
 * <p/>
 * Класс - просто сборник различных кейсов для юзера
 * login: tredovitchbytimestamprange
 * pwd: testqa
 * (newthreadsbytimestamprangetest, threadsince)
 * с заранее заготовленными письмами
 *  с таймстемпами:
 *
 * 1427201893
 * 1427202087
 * 1427202447
 *
 * и 2 треда
 * с таймстемпами
 *
 * 1427201893
 * 1427202447
 *
 * (берется последнее письмо)
 */
public class TimestampData {

    public static final Integer MSG_TIMESTAMP_1 = 1427201893;
    public static final Integer MSG_TIMESTAMP_2 = 1427202087;
    public static final Integer MSG_TIMESTAMP_3 = 1427202447;

    public static final Integer THR_TIMESTAMP_1 = 1427201893;
    public static final Integer THR_TIMESTAMP_2 = 1427202447;

    public static final Integer FUTURE = MSG_TIMESTAMP_3 + 10000000;

    //TEST DATA: since, till, expected
    public static Collection<Object[]> timestampDataForLetters() {
        ArrayList<Object[]> data = new ArrayList<Object[]>();
        //OK:  > проверяемое значение <
        data.add(new Object[]{0, 0, 3});
        data.add(new Object[]{0, MSG_TIMESTAMP_1, 0});
        data.add(new Object[]{0, MSG_TIMESTAMP_1 + 1, 1});
        data.add(new Object[]{0, MSG_TIMESTAMP_1 + 2, 1});

        //MAILPG-379 стало: from <= date < to;
        ///from <= date
        data.add(new Object[]{MSG_TIMESTAMP_1, MSG_TIMESTAMP_1 + 1, 1});
        //date < to;
        data.add(new Object[]{MSG_TIMESTAMP_1 - 1, MSG_TIMESTAMP_1, 0});

        data.add(new Object[]{MSG_TIMESTAMP_1 - 1, MSG_TIMESTAMP_1 + 1, 1});
        data.add(new Object[]{MSG_TIMESTAMP_2 - 1, MSG_TIMESTAMP_2 + 1, 1});

        data.add(new Object[]{MSG_TIMESTAMP_1 + 1, 0, 2});

        data.add(new Object[]{MSG_TIMESTAMP_1 + 1, MSG_TIMESTAMP_1 + 1, 0});

        data.add(new Object[]{MSG_TIMESTAMP_2 - 1, MSG_TIMESTAMP_2 + 1, 1});
        data.add(new Object[]{MSG_TIMESTAMP_1 - 1, FUTURE, 3});

        //since > till
        //DARIA-45776 не должны делать запрос к бэкэнду
        data.add(new Object[]{FUTURE, MSG_TIMESTAMP_2 + 1, 0});

        data.add(new Object[]{FUTURE, FUTURE, 0});

        data.add(new Object[]{FUTURE, null, 0});
        data.add(new Object[]{null, FUTURE, 3});

        data.add(new Object[]{MSG_TIMESTAMP_1 + 1, 1, 0});

        return data;
    }

    //TEST DATA: since, till, expected
    public static Collection<Object[]> timestampDataForThreads() {
        ArrayList<Object[]> data = new ArrayList<Object[]>();

        //OK:  > проверяемое значение <
        data.add(new Object[]{0, 0, 2});
        data.add(new Object[]{0, THR_TIMESTAMP_1, 0});

        //MAILPG-379 стало: from <= date < to;
        ///from <= date
        data.add(new Object[]{0, THR_TIMESTAMP_1 + 1, 1});

        data.add(new Object[]{0, THR_TIMESTAMP_1 + 2, 1});

        data.add(new Object[]{THR_TIMESTAMP_1, THR_TIMESTAMP_1 + 1, 1});
        //date < to;
        data.add(new Object[]{THR_TIMESTAMP_1 - 1, THR_TIMESTAMP_1, 0});

        //[DARIA-35380]
        //data.add(new Object[]{"1391600220", "0", 1});

        data.add(new Object[]{THR_TIMESTAMP_1 + 1, THR_TIMESTAMP_1  + 1, 0});

        data.add(new Object[]{MSG_TIMESTAMP_2 - 1, MSG_TIMESTAMP_2 + 1, 0});

        data.add(new Object[]{THR_TIMESTAMP_2 - 1, THR_TIMESTAMP_2 + 1, 1});

        data.add(new Object[]{THR_TIMESTAMP_1 - 1, FUTURE, 2});

        //since > till
        data.add(new Object[]{FUTURE, MSG_TIMESTAMP_2 + 1, 0});

        data.add(new Object[]{FUTURE, FUTURE, 0});

        data.add(new Object[]{FUTURE, null, 0});
        data.add(new Object[]{null, FUTURE, 2});

        data.add(new Object[]{THR_TIMESTAMP_1 + 1, 1, 0});

        return data;
    }

    //TEST DATA: since, and_more, expected
    public static Collection<Object[]> timestampDataForAndMoreMessages() {
        ArrayList<Object[]> data = new ArrayList<Object[]>();

        data.add(new Object[]{null, 3, 3});

        data.add(new Object[]{MSG_TIMESTAMP_1 - 1, 0, 3});
        data.add(new Object[]{MSG_TIMESTAMP_1 - 1, 1, 3});

        data.add(new Object[]{MSG_TIMESTAMP_2 - 1, 1, 3});
        data.add(new Object[]{MSG_TIMESTAMP_2 - 1, 10, 3});

        data.add(new Object[]{MSG_TIMESTAMP_2 - 1, 0, 2});
        data.add(new Object[]{MSG_TIMESTAMP_2 - 1, 1, 3});
        data.add(new Object[]{MSG_TIMESTAMP_2 - 1, 10, 3});

        data.add(new Object[]{MSG_TIMESTAMP_3 - 1, 0, 1});

        //DARIA-35732
        data.add(new Object[]{MSG_TIMESTAMP_3 - 1, 1, 2});
        data.add(new Object[]{MSG_TIMESTAMP_3 - 1, 2, 3});
        data.add(new Object[]{MSG_TIMESTAMP_3 - 1, 10, 3});

        data.add(new Object[]{MSG_TIMESTAMP_3 + 2, 0, 0});
        data.add(new Object[]{MSG_TIMESTAMP_3 + 2, 1, 1});
        data.add(new Object[]{MSG_TIMESTAMP_3 + 2, 2, 2});
        data.add(new Object[]{MSG_TIMESTAMP_3 + 2, 3, 3});
        //DARIA-45776
        data.add(new Object[]{MSG_TIMESTAMP_3 + 2, 6000, 3});


        return data;
    }

    //TEST DATA: since, and_more, expected
    public static Collection<Object[]> timestampDataForAndMoreThreads() {
        ArrayList<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{null, 2, 2});

        data.add(new Object[]{THR_TIMESTAMP_1 - 1, 0, 2});
        data.add(new Object[]{THR_TIMESTAMP_1 - 1, 1, 2});

        data.add(new Object[]{THR_TIMESTAMP_2 - 1, 0, 1});
        data.add(new Object[]{THR_TIMESTAMP_2 - 1, 1, 2});
        data.add(new Object[]{THR_TIMESTAMP_2 - 1, 10, 2});

        data.add(new Object[]{THR_TIMESTAMP_2 + 2, 0, 0});
        data.add(new Object[]{THR_TIMESTAMP_2 + 2 , 1, 1});
        data.add(new Object[]{THR_TIMESTAMP_2 + 2, 2, 2});
        //DARIA-45776
        data.add(new Object[]{THR_TIMESTAMP_2 + 2, 6000, 2});

        return data;
    }
}
