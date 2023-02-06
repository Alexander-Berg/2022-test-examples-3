package ru.yandex.market.core.testing;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

import ru.yandex.market.core.annotations.ConverterClass;
import ru.yandex.market.core.framework.converter.SmartStandartBeanElementConverter;

/**
 * Учетная карточка тестирования магазина. В ней хранится состояние прохождения магазином премодерации.
 */
@ConverterClass(SmartStandartBeanElementConverter.class)
public class TestingState implements Serializable {

    /**
     * Идентификатор записи.
     */
    private long id;

    private long datasourceId;
    /**
     * Магазин готов к проверке (нажал кнопку "Ошибки исправлены"/"Отправить на проверку" и тд).
     * Флаг действителен, когда установлен флаг {@link #approved}.
     */
    private boolean ready;
    /**
     * Флаг используется для подтверждения флагов<ul>
     * <li>{@link #ready} - TODO понять почему сразу не отправляется в тестинг </li>
     * <li>{@link #cancelled} - для того,
     * чтобы магазин после провала премодерации некоторое время висел в тестинге.</li>
     * </ul>
     */
    private boolean approved;
    /**
     * Магазин в данный момент находится на тестировании. Флаг устанавливается для магазинов, у которых установлены
     * флаги {@link #ready} и {@link #approved}.
     *
     * @see ru.yandex.market.core.param.model.ParamType#SHOP_TESTING Параметр магазина SHOP_TESTING
     */
    private boolean inProgress;
    /**
     * Проверка провалена. Флаг действителен, когда установлен флаг {@link #approved}.
     */
    private boolean cancelled;
    /**
     * Магазин не смог пройти премодерацию из-за какой-то серьёзной ошибки (сайт был недоступен долгое время и тд).
     * Флаг не позволяет магазину проходить премодерацию до тех пор, пока менеджер Яндекса не сбросит этот флаг
     * в админке.
     */
    private boolean fatalCancelled;

    /**
     * Кол-во штрафных попыток пройти проверку.
     */
    private int pushReadyButtonCount;

    private String recommendations;

    /**
     * Дата начала проверки магазина. Если дата не установлена, то проверка никогда не начнется.
     */
    private Date startDate;

    /**
     * Дата последнего изменения карточки.
     */
    private Date updatedAt;
    /**
     * Тип текущей проверки. Проверка осуществляться как со стороны Маркета, так и со стороны магазина - {@link
     * TestingType#SELF_CHECK}.
     */
    private TestingType testingType;

    /**
     * Идентификатор отключения из-за которого магазин попал на проверку.
     */
    private long cutoffId;

    private TestingStatus status;

    private boolean cloneCheckRequired;

    private boolean qualityCheckRequired;

    /**
     * Текущий номер попытки пройти проверку. Обнуляется только в случае успешного прохождения проверки.
     */
    private int attemptNum;

    /**
     * Номер итерации (каждые шесть неудачных попыток - новая итерация).
     */
    private int iterationNum;

    public TestingState() {
    }

    public TestingState(TestingState state) {
        this(state.getId(),
                state.getDatasourceId(),
                state.isReady(),
                state.isApproved(),
                state.isInProgress(),
                state.isCancelled(),
                state.getPushReadyButtonCount(),
                state.isFatalCancelled(),
                state.getRecommendations(),
                state.getStartDate(),
                state.getUpdatedAt(),
                state.getTestingType(),
                state.getCutoffId(),
                state.isCloneCheckRequired(),
                state.isQualityCheckRequired(),
                state.getStatus(),
                state.getAttemptNum(),
                state.getIterationNum());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public TestingState(long id,
                        long datasourceId,
                        boolean ready,
                        boolean approved,
                        boolean inProgress,
                        boolean cancelled,
                        int readyButtonPushCount,
                        boolean fatalCancelled,
                        String recommendations,
                        Date startDate,
                        Date updatedAt,
                        TestingType testingType,
                        long cutoffId,
                        boolean cloneCheckRequired,
                        boolean qualityCheckRequired,
                        TestingStatus status,
                        int attemptNum,
                        int iterationNum) {
        this.id = id;
        this.datasourceId = datasourceId;
        this.ready = ready;
        this.approved = approved;
        this.inProgress = inProgress;
        this.cancelled = cancelled;
        this.pushReadyButtonCount = readyButtonPushCount;
        this.fatalCancelled = fatalCancelled;
        this.recommendations = recommendations;
        this.startDate = startDate;
        this.updatedAt = updatedAt;
        this.testingType = testingType;
        this.cutoffId = cutoffId;
        this.cloneCheckRequired = cloneCheckRequired;
        this.qualityCheckRequired = qualityCheckRequired;
        this.status = status;
        this.attemptNum = attemptNum;
        this.iterationNum = iterationNum;
    }

    public boolean needInfo() {
        return status == TestingStatus.NEED_INFO;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDatasourceId() {
        return datasourceId;
    }

    public TestingState setDatasourceId(long datasourceId) {
        this.datasourceId = datasourceId;
        return this;
    }

    public boolean isReady() {
        return ready;
    }

    public TestingState setReady(boolean ready) {
        this.ready = ready;
        return this;
    }

    public boolean isApproved() {
        return approved;
    }

    public TestingState setApproved(boolean approved) {
        this.approved = approved;
        return this;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public TestingState setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
        return this;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public TestingState setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        return this;
    }

    public int getPushReadyButtonCount() {
        return pushReadyButtonCount;
    }

    @Nonnull
    public TestingState setPushReadyButtonCount(int pushReadyButtonCount) {
        this.pushReadyButtonCount = pushReadyButtonCount;
        return this;
    }

    public boolean isNumberOfAttemptsExceeded(int maxAttempts) {
        return pushReadyButtonCount >= maxAttempts;
    }

    public boolean isAttemptCountRequired() {
        return testingType == TestingType.FULL_PREMODERATION || testingType == TestingType.CPC_PREMODERATION;
    }

    public boolean isFatalCancelled() {
        return fatalCancelled;
    }

    public void setFatalCancelled(boolean fatalCancelled) {
        this.fatalCancelled = fatalCancelled;
    }

    /**
     * Выставляются только при получении результатов премодерации от ABO.
     * <p>
     * В случае если премодерации не было, например, лайт-проверка, то будет null.
     */
    @Nullable
    public String getRecommendations() {
        return recommendations;
    }

    @Nonnull
    public TestingState setRecommendations(@Nullable String recommendations) {
        this.recommendations = recommendations;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public TestingState setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }


    public boolean canPushReadyButton() {
        return (!ready && !inProgress && !needManagerApprove())
                || testingType == TestingType.SELF_CHECK
                || testingType == TestingType.API_DEBUG;
    }

    public boolean needManagerApprove() {
        return startDate == null || fatalCancelled;
    }

    public TestingType getTestingType() {
        return testingType;
    }

    @Nonnull
    public TestingState setTestingType(TestingType testingType) {
        this.testingType = testingType;
        return this;
    }

    public Date getStartDate() {
        return startDate;
    }

    @Nonnull
    public TestingState setStartDate(@Nullable Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public long getCutoffId() {
        return cutoffId;
    }

    public TestingState setCutoffId(long cutoffId) {
        this.cutoffId = cutoffId;
        return this;
    }

    /**
     * Объект не был "загружен из базы", а был создан новым. Это означает, что никакой записи о состоянии магазина в
     * тестинге нет
     */
    public boolean isPhantom() {
        return id == 0;
    }

    public boolean isCloneCheckRequired() {
        return cloneCheckRequired;
    }

    public TestingState setCloneCheckRequired(boolean cloneCheckRequired) {
        this.cloneCheckRequired = cloneCheckRequired;
        return this;
    }

    public boolean isQualityCheckRequired() {
        return qualityCheckRequired;
    }

    public TestingState setQualityCheckRequired(boolean qualityCheckRequired) {
        this.qualityCheckRequired = qualityCheckRequired;
        return this;
    }

    public TestingStatus getStatus() {
        return status;
    }

    public TestingState setStatus(TestingStatus status) {
        this.status = status;
        return this;
    }

    public int getAttemptNum() {
        return attemptNum;
    }

    public TestingState setAttemptNum(int attemptNum) {
        this.attemptNum = attemptNum;
        return this;
    }

    public boolean isFirstAttempt() {
        return attemptNum == 0;
    }

    public void incAttemptNum() {
        attemptNum++;
    }

    public int getIterationNum() {
        return iterationNum;
    }

    public TestingState setIterationNum(int iterationNum) {
        this.iterationNum = iterationNum;
        return this;
    }

    /**
     * @return true, если при отправлении нотификации в данном TestingState
     * необходимо отправлять рекомендации по настройке размещения магазину.
     */
    public boolean needSendSettingsRecommendations() {
        return testingType == TestingType.CPC_PREMODERATION && status == TestingStatus.PASSED;
    }

    public boolean isInProgressOrWaiting() {
        return inProgress || TestingStatus.PENDING_CHECK_START == status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestingState that = (TestingState) o;
        return id == that.id &&
                datasourceId == that.datasourceId &&
                ready == that.ready &&
                approved == that.approved &&
                inProgress == that.inProgress &&
                cancelled == that.cancelled &&
                fatalCancelled == that.fatalCancelled &&
                pushReadyButtonCount == that.pushReadyButtonCount &&
                cutoffId == that.cutoffId &&
                cloneCheckRequired == that.cloneCheckRequired &&
                qualityCheckRequired == that.qualityCheckRequired &&
                Objects.equals(recommendations, that.recommendations) &&
                Objects.equals(startDate, that.startDate) &&
                Objects.equals(updatedAt, that.updatedAt) &&
                testingType == that.testingType &&
                status == that.status &&
                attemptNum == that.attemptNum &&
                iterationNum == that.iterationNum;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("datasourceId", datasourceId)
                .add("ready", ready)
                .add("approved", approved)
                .add("inProgress", inProgress)
                .add("cancelled", cancelled)
                .add("fatalCancelled", fatalCancelled)
                .add("pushReadyButtonCount", pushReadyButtonCount)
                .add("recommendations", recommendations)
                .add("startDate", startDate)
                .add("updatedAt", updatedAt)
                .add("testingType", testingType)
                .add("cutoffId", cutoffId)
                .add("status", status)
                .add("cloneCheckRequired", cloneCheckRequired)
                .add("qualityCheckRequired", qualityCheckRequired)
                .add("attemptNum", attemptNum)
                .add("iterationNum", iterationNum)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                datasourceId,
                ready,
                approved,
                inProgress,
                cancelled,
                fatalCancelled,
                pushReadyButtonCount,
                recommendations,
                startDate,
                updatedAt,
                testingType,
                cutoffId,
                status,
                cloneCheckRequired,
                qualityCheckRequired,
                attemptNum,
                iterationNum);
    }
}
