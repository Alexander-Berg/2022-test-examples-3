package ru.yandex.autotests.innerpochta.rules.acclock;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.yandex.autotests.common.mongoapi.MongoRule;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.yandex.autotests.innerpochta.util.props.AccountsProperties.accounts;

/**
 * User: lanwen
 * Date: 03.09.13
 * Time: 20:02
 */
public class AccLockRule extends TestWatcher {

    public static final int MAX_WAIT_CYCLES = 100;
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final List<String> names = new ArrayList<>();
    private final List<String> groups = accounts().groups();
    private final Map<String, String> nameGroupMap = new HashMap<>();
    private MongoRule mongo;

    private int waitTime = 0;

    private boolean annotation = false;
    private boolean classname = false;
    private boolean ignoreLock = true;
    public static boolean useTusAccount = false;
    public int accNumber = 1;
    public static String[] tags;

    private final TestUserService user = new TestUserService();

    private List<Account> tusAccList = new ArrayList<>();

    public static AccLockRule use() {
        return new AccLockRule();
    }

    /**
     * Использует четко заданные имена аккаунтов для лока
     *
     * @param names
     * @return
     */
    public AccLockRule names(String... names) {
        this.names.addAll(Arrays.asList(names));
        return this;
    }

    /**
     * Включает поиск аннотаций @UseCreds({...}) на методах и классах для добавления имен аккаунтов
     *
     * @return this
     */
    public AccLockRule annotation() {
        this.annotation = true;
        return this;
    }

    public AccLockRule className() {
        this.classname = true;
        return this;
    }

    public AccLockRule useTusAccount(int number) {
        useTusAccount = true;
        accNumber = number;
        return this;
    }

    public AccLockRule usePreloadedTusAccounts(int number) {
        useTusAccount = true;
        accNumber = 0;
        for (int i = 0; i < number; i++) {
            tusAccList.add(user.getAcc());
        }
        return this;
    }

    public AccLockRule createAndUseTusAccount() {
        useTusAccount = true;
        accNumber = 0;
        tusAccList.add(user.createAndLockAcc());
        return this;
    }

    public AccLockRule generateTusAccount(int number) {
        for (int i = 0; i < number; i++) {
            user.createAcc();
        }
        return this;
    }

    public AccLockRule generateTusAccount(int number, String... tags) {
        for (int i = 0; i < number; i++) {
            user.createAcc(tags);
        }
        return this;
    }

    public AccLockRule useTusAccount() {
        return useTusAccount(1);
    }

    public AccLockRule useTusAccount(String... tag) {
        useTusAccount = true;
        AccLockRule.tags = tag;
        return this;
    }

    /**
     * Если используется, выключает все попытки заблокировать или проверить на залоченность акк
     *
     * @return this
     */
    public AccLockRule ignoreLock() {
        this.ignoreLock = true;
        return this;
    }


    public Account acc(String name) {
        String group = currentGroupFor(name);
        if (isEmpty(group)) {
            throw new IllegalStateException("Should use 'ignoreLock()' and wait of 'starting()' method" +
                " or lock some group first for: " + name);
        }
        return accounts().account(group, name);
    }

    public Account accNum(int num) {
        return tusAccList.get(num);
    }

    public List<Account> allAccs() {
        return tusAccList;
    }

    public Account firstAcc() {
        if (tusAccList.isEmpty() & useTusAccount) {
            tusAccList.add(user.getAcc());
            return tusAccList.get(0);
        } else if (useTusAccount) {
            return tusAccList.get(0);
        }
        if (names.isEmpty()) {
            throw new IllegalStateException("Should add some names with 'names(...)' or '@UseCreds({...})'" +
                " before can fetch first");
        }
        return acc(names.get(0));
    }


    @Override
    protected void starting(Description description) {
        user.setTestClassName(description.getTestClass().getSimpleName());
        if (annotation && (methodHasAnnotation(description) || classHasAnnotation(description))) {
            String[] namesFromAnno = getNameFromAnnotation(description);
            names(namesFromAnno);
        }

        if (classname) {
            names(description.getTestClass().getSimpleName());
        }

        if (useTusAccount) {
            for (int i = 0; i < accNumber; i++) {
                if (tags == null) {
                    tusAccList.add(user.getAcc());
                } else tusAccList.add(user.getAcc(tags));
            }
        }

        if (!useTusAccount) {
            for (String name : names) {
                selectNewGroupFor(name);
                lock(name);
            }
        }
    }

    @Override
    protected void finished(Description description) {
        if (useTusAccount) {
            for (Account account : tusAccList) {
                user.unlockAcc(Utils.getUserUid(account.getLogin()));
            }
            tusAccList = new ArrayList<>();
//            if (tusAccList.isEmpty()) {
//                user.unlockAcc(Utils.getUserUid(firstAcc().getLogin()));
//            }
        }
        for (String name : names) {
            unlock(name);
        }
    }

    public AccLockRule lock(String name) {
        if (ignoreLock) {
            return this;
        }
        while (isLocked(name) || waitTime > MAX_WAIT_CYCLES) {
            selectNewGroupFor(name);
            waitFor(waitTime++);
        }
        AccLocker accLocker = new AccLocker(name, currentGroupFor(name));
        mongoInstance().insert(accLocker, 10, TimeUnit.MINUTES);
        logger.info(String.format("Locked acc: %s in: %s", name, currentGroupFor(name)));
        return this;
    }

    /**
     * Разблочит имя только в том случае, если последняя смена пары совпадает с нужным локом
     *
     * @param name - имя акка
     */
    public void unlock(String name) {
        if (ignoreLock) {
            return;
        }
        mongoInstance().remove(AccLocker.query(name, currentGroupFor(name)));
        logger.info(String.format("UnLocked acc: %s in: %s", name, currentGroupFor(name)));
    }

    private String[] getNameFromAnnotation(Description description) {
        if (methodHasAnnotation(description)) {
            return description.getAnnotation(UseCreds.class).value();
        }
        return description.getTestClass().getAnnotation(UseCreds.class).value();
    }

    private boolean methodHasAnnotation(Description description) {
        return description.getAnnotation(UseCreds.class) != null;
    }


    private boolean classHasAnnotation(Description description) {
        return description.getTestClass().isAnnotationPresent(UseCreds.class);
    }

    private boolean isLocked(String name) {
        logger.info(String.format("Try acc: %s in: %s", name, currentGroupFor(name)));
        return !mongoInstance().find(AccLocker.query(name, currentGroupFor(name))).isEmpty();
    }

    private String currentGroupFor(String name) {
        return nameGroupMap.get(name);
    }

    private void selectNewGroupFor(String name) {
        List<String> rdyToLock = new LinkedList<>(groups);
        if (rdyToLock.size() == 1) {
            nameGroupMap.put(name, rdyToLock.get(0));
            return;
        }
        rdyToLock.remove(currentGroupFor(name));
        int index = (int) (Math.random() * rdyToLock.size());
        nameGroupMap.put(name, rdyToLock.get(index));
    }


    private void waitFor(int time) {
        try {
            logger.info(String.format("Wait... %d (%s)", time, TimeUnit.SECONDS));
            Thread.sleep(TimeUnit.SECONDS.toMillis(time));
        } catch (InterruptedException e) {
            throw new RuntimeException("", e);
        }
    }

    private synchronized MongoRule mongoInstance() {
        if (null == mongo) {
            mongo = new MongoRule(AccLockRule.class);
        }
        return mongo;
    }


}
