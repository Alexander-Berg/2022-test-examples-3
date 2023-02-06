package ui_tests.src.test.java.Classes;

import java.util.List;
import java.util.Objects;

public class ImportsSummary {
    private String title;
    private String received;
    private String skipped;
    private String successful;
    private String created;
    private String updated;
    private String failed;
    private List<String> failedIds;
    private String gid;

    public String getGid() {
        return gid;
    }

    public ImportsSummary setGid(String gid) {
        this.gid = gid;
        return this;
    }

    /**
     * Получить название результата импорта
     *
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * указать название результата импорта
     *
     * @param title
     * @return
     */
    public ImportsSummary setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Получить значение поля "Строк получено"
     *
     * @return
     */
    public String getReceived() {
        return received;
    }

    /**
     * Указать значение поля "Строк получено"
     *
     * @param received
     * @return
     */
    public ImportsSummary setReceived(String received) {
        this.received = received;
        return this;
    }

    /**
     * Получить значение поля "Строк пропущено"
     *
     * @return
     */
    public String getSkipped() {
        return skipped;
    }

    /**
     * Указать значение поля "Строк пропущено"
     *
     * @param skipped
     * @return
     */
    public ImportsSummary setSkipped(String skipped) {
        this.skipped = skipped;
        return this;
    }

    /**
     * Получить значение поля "Успешно"
     *
     * @return
     */
    public String getSuccessful() {
        return successful;
    }

    /**
     * Указать значение поля "Успешно"
     *
     * @param successful
     * @return
     */
    public ImportsSummary setSuccessful(String successful) {
        this.successful = successful;
        return this;
    }

    /**
     * Получить значение поля "Создано"
     *
     * @return
     */
    public String getCreated() {
        return created;
    }

    /**
     * Указать значение поля "Создано"
     *
     * @param created
     * @return
     */
    public ImportsSummary setCreated(String created) {
        this.created = created;
        return this;
    }

    /**
     * Получить значение поля "Изменено"
     *
     * @return
     */
    public String getUpdated() {
        return updated;
    }

    /**
     * Указать значение поля "изменено"
     *
     * @param updated
     * @return
     */
    public ImportsSummary setUpdated(String updated) {
        this.updated = updated;
        return this;
    }

    /**
     * Получить значение поля "С ошибкой"
     *
     * @return
     */
    public String getFailed() {
        return failed;
    }

    /**
     * Указать значение поля "С ошибкой"
     *
     * @param failed
     * @return
     */
    public ImportsSummary setFailed(String failed) {
        this.failed = failed;
        return this;
    }

    /**
     * Получить значение поля "Id неудачных строк"
     *
     * @return
     */
    public List<String> getFailedIds() {
        return failedIds;
    }

    /**
     * Указать значение поля "Id неудачных строк"
     *
     * @param failedIds
     * @return
     */
    public ImportsSummary setFailedIds(List<String> failedIds) {
        this.failedIds = failedIds;
        return this;
    }

    @Override
    public String toString() {
        return "ImportsSummary{" +
                "title='" + title + '\'' +
                ", received='" + received + '\'' +
                ", skipped='" + skipped + '\'' +
                ", successful='" + successful + '\'' +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                ", failed='" + failed + '\'' +
                ", failedIds=" + failedIds +
                ", gid='" + gid + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportsSummary anImport = (ImportsSummary) o;
        if (this.title != null) {
            if (!this.title.equals(anImport.getTitle())) {
                return false;
            }
        }
        if (this.received != null) {
            if (!this.received.equals(anImport.getReceived())) {
                return false;
            }
        }
        if (this.skipped != null) {
            if (!this.skipped.equals(anImport.getSkipped())) {
                return false;
            }
        }
        if (this.successful != null) {
            if (!this.successful.equals(anImport.getSuccessful())) {
                return false;
            }
        }
        if (this.created != null) {
            if (!this.created.equals(anImport.getCreated())) {
                return false;
            }
        }
        if (this.updated != null) {
            if (!this.updated.equals(anImport.getUpdated())) {
                return false;
            }
        }
        if (this.failed != null) {
            if (!this.failed.equals(anImport.getFailed())) {
                return false;
            }
        }
        if (this.failedIds != null) {
            if (this.failedIds.size() != anImport.getFailedIds().size()) {
                return false;
            }
            if (!this.failedIds.containsAll(anImport.getFailedIds())){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, received, skipped, successful, created, updated, failed, failedIds);
    }
}
