package ui_tests.src.test.java.Classes;


import java.util.List;
import java.util.regex.Pattern;

public class Import implements Cloneable {
    private String filePath;
    private String status;
    private String gid;
    private String progress;
    private String commentText;
    private ImportsSummary importsSummary;
    private String summaryTitle;
    private String summaryLink;
    private String configurationImportTitle;
    private String linkOnConfigurationImportPage;
    private String SourcesFileTitle;
    private String linkOnSourcesFile;
    private String logsFileTitle;
    private String linkOnLogsFile;
    private String serviceTitle;
    private String linkOnServicePage;
    private List<String> categories;

    @Override
    public String toString() {
        return "Import{" +
                "filePath='" + filePath + '\'' +
                ", status='" + status + '\'' +
                ", gid='" + gid + '\'' +
                ", progress='" + progress + '\'' +
                ", commentText='" + commentText + '\'' +
                ", importsSummary=" + importsSummary +
                ", summaryTitle='" + summaryTitle + '\'' +
                ", summaryLink='" + summaryLink + '\'' +
                ", configurationImportTitle='" + configurationImportTitle + '\'' +
                ", linkOnConfigurationImportPage='" + linkOnConfigurationImportPage + '\'' +
                ", SourcesFileTitle='" + SourcesFileTitle + '\'' +
                ", linkOnSourcesFile='" + linkOnSourcesFile + '\'' +
                ", logsFileTitle='" + logsFileTitle + '\'' +
                ", linkOnLogsFile='" + linkOnLogsFile + '\'' +
                ", serviceTitle='" + serviceTitle + '\'' +
                ", linkOnServicePage='" + linkOnServicePage + '\'' +
                ", categories=" + categories +
                '}';
    }

    public List<String> getCategories() {
        return categories;
    }

    public Import setCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    /**
     * Получить заголовок очереди
     * @return
     */
    public String getServiceTitle() {
        return serviceTitle;
    }

    /**
     * Указать заголовок очереди
     * @param serviceTitle
     * @return
     */
    public Import setServiceTitle(String serviceTitle) {
        this.serviceTitle = serviceTitle;
        return this;
    }

    /**
     * получить ссылку на очередь
     * @return
     */
    public String getLinkOnServicePage() {
        return linkOnServicePage;
    }

    /**
     * Указать ссылку на очередь
     * @param linkOnServicePage
     * @return
     */
    public Import setLinkOnServicePage(String linkOnServicePage) {
        this.linkOnServicePage = linkOnServicePage;
        return this;
    }

    /**
     * Вернуть дубликат текущей записи
     * @return
     */
    @Override
    public Import clone(){
        try {
            return (Import) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new Import();
    }

    /**
     * Получить заголовок результатов импорта
     *
     * @return
     */
    public String getSummaryTitle() {
        return summaryTitle;
    }

    /**
     * Указать заголовок результата импорта
     *
     * @param summaryTitle
     * @return
     */
    public Import setSummaryTitle(String summaryTitle) {
        this.summaryTitle = summaryTitle;
        return this;
    }

    /**
     * Получить ссылку на страницу результата импорта либо регулярку на формирование этой ссылки
     *
     * @return
     */
    public String getSummaryLink() {
        return summaryLink;
    }

    /**
     * Указать ссылку на результат импорта либо регулярку на формирование этой ссылки
     *
     * @param summaryLink
     * @return
     */
    public Import setSummaryLink(String summaryLink) {
        this.summaryLink = summaryLink;
        return this;
    }

    /**
     * Получить заголовок кончигурации импорта либо регулярку на формирование этой ссылки
     *
     * @return
     */
    public String getConfigurationImportTitle() {
        return configurationImportTitle;
    }

    /**
     * Указать заголовок конфигурации импорта либо регулярку на формирование этой ссылки
     *
     * @param configurationImportTitle
     * @return
     */
    public Import setConfigurationImportTitle(String configurationImportTitle) {
        this.configurationImportTitle = configurationImportTitle;
        return this;
    }

    /**
     * Получить ссылку на конфигурацию импорта либо регулярку на формирование этой ссылки
     *
     * @return
     */
    public String getLinkToConfigurationImportPage() {
        return linkOnConfigurationImportPage;
    }

    /**
     * Указать заголовок на конфигурацию импорта либо регулярку на формирование этой ссылки
     *
     * @param linkOnConfigurationImportPage
     * @return
     */
    public Import setLinkOnConfigurationImportPage(String linkOnConfigurationImportPage) {
        this.linkOnConfigurationImportPage = linkOnConfigurationImportPage;
        return this;
    }

    /**
     * Получить заголовок файла с данными либо регулярку на формирование этой ссылки
     *
     * @return
     */
    public String getSourcesFileTitle() {
        return SourcesFileTitle;
    }

    /**
     * Указать заголовок файла с данными либо регулярку на формирование этой ссылки
     *
     * @param sourcesFileTitle
     * @return
     */
    public Import setSourcesFileTitle(String sourcesFileTitle) {
        this.SourcesFileTitle = sourcesFileTitle;
        return this;
    }

    /**
     * Получить ссылку на файл с данными либо регулярку на формирование этой ссылки
     *
     * @return
     */
    public String getLinkOnSourcesFile() {
        return linkOnSourcesFile;
    }

    /**
     * Указать ссылку на файл с данными либо регулярку на формирование этой ссылки
     *
     * @param linkOnSourcesFile
     * @return
     */
    public Import setLinkOnSourcesFile(String linkOnSourcesFile) {
        this.linkOnSourcesFile = linkOnSourcesFile;
        return this;
    }

    /**
     * Получить заголовок файла с логами либо регулярку на формирование этой ссылки
     *
     * @return
     */
    public String getLogsFileTitle() {
        return logsFileTitle;
    }

    /**
     * Указать заголовок файла с логами либо регулярку на формирование этой ссылки
     *
     * @param logsFileTitle
     * @return
     */
    public Import setLogsFileTitle(String logsFileTitle) {
        this.logsFileTitle = logsFileTitle;
        return this;
    }

    /**
     * Получить ссылку на файл с логами либо регулярку на формирование этой ссылки
     *
     * @return
     */
    public String getLinkOnLogsFile() {
        return linkOnLogsFile;
    }

    /**
     * Указать ссылку на файл с логами либо регулярку на формирование этой ссылки
     *
     * @param linkOnLogsFile
     * @return
     */
    public Import setLinkOnLogsFile(String linkOnLogsFile) {
        this.linkOnLogsFile = linkOnLogsFile;
        return this;
    }

    /**
     * Получить результаты импорта
     *
     * @return
     */
    public ImportsSummary getImportsSummary() {
        return importsSummary;
    }

    /**
     * Указать результаты импорта
     *
     * @param importsSummary
     * @return
     */
    public Import setImportsSummary(ImportsSummary importsSummary) {
        this.importsSummary = importsSummary;
        return this;
    }

    /**
     * Получить текст комментария
     *
     * @return
     */
    public String getCommentText() {
        return commentText;
    }

    /**
     * Указать текст комментария
     *
     * @param commentText
     * @return
     */
    public Import setCommentText(String commentText) {
        this.commentText = commentText;
        return this;
    }

    /**
     * Получить количество обработанных строк
     *
     * @return
     */
    public String getProgress() {
        return progress;
    }

    /**
     * Указать количество обработанных строк
     *
     * @param progress
     * @return
     */
    public Import setProgress(String progress) {
        this.progress = progress;
        return this;
    }

    /**
     * Получить gid импорта
     *
     * @return
     */
    public String getGid() {
        return gid;
    }

    /**
     * указать gid импорта
     *
     * @param gid
     * @return
     */
    public Import setGid(String gid) {
        this.gid = gid;
        return this;
    }

    /**
     * Получить статус импорта
     *
     * @return
     */
    public String getStatus() {
        return status;
    }

    /**
     * Указать статус импорта
     *
     * @param status
     * @return
     */
    public Import setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Поучить путь до файла импорта
     *
     * @return
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Указать пусть до файла импорта
     *
     * @param filePath
     * @return
     */
    public Import setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        Import anImport = (Import) o;
        if (this.status != null) {
            if (!this.status.equals(anImport.getStatus())) {
                return false;
            }
        }

        if (this.progress != null) {
            if (!this.progress.equals(anImport.getProgress())) {
                return false;
            }
        }

        if (this.commentText != null) {
            if (!this.commentText.equals(anImport.getCommentText())) {
                return false;
            }
        }

        if (this.importsSummary != null) {
            if (!this.importsSummary.equals(anImport.getImportsSummary())) {
                return false;
            }
        }

        if (this.summaryTitle!=null){
            if (!this.summaryTitle.equals(anImport.getSummaryTitle())){
                Pattern pattern = Pattern.compile(this.summaryTitle);
                if (!pattern.matcher(anImport.getSummaryTitle()).find()){
                    return false;
                }
            }
        }

        if (this.summaryLink!=null){
            if(!this.summaryLink.equals(anImport.getSummaryLink())){
                Pattern pattern = Pattern.compile(this.summaryLink);
                if (!pattern.matcher(anImport.getSummaryLink()).find()){
                    return false;
                }
            }
        }

        if (this.configurationImportTitle !=null){
            if (!this.configurationImportTitle.equals(anImport.getConfigurationImportTitle())){
                Pattern pattern = Pattern.compile(this.configurationImportTitle);
                if (!pattern.matcher(anImport.getConfigurationImportTitle()).find()){
                    return false;
                }
            }
        }

        if (this.linkOnConfigurationImportPage!=null){
            if (!this.linkOnConfigurationImportPage.equals(anImport.getLinkToConfigurationImportPage())){
                Pattern pattern = Pattern.compile(this.linkOnConfigurationImportPage);
                if (!pattern.matcher(anImport.getLinkToConfigurationImportPage()).find()){
                    return false;
                }
            }
        }

        if (this.SourcesFileTitle !=null){
            if (!this.SourcesFileTitle.equals(anImport.getSourcesFileTitle())){
                Pattern pattern = Pattern.compile(this.SourcesFileTitle);
                if (!pattern.matcher(anImport.getSourcesFileTitle()).find()){
                    return false;
                }
            }
        }

        if (this.linkOnSourcesFile!=null){
            if (!this.linkOnSourcesFile.equals(anImport.getLinkOnSourcesFile())){
                Pattern pattern = Pattern.compile(this.linkOnSourcesFile);
                if (!pattern.matcher(anImport.getLinkOnSourcesFile()).find()){
                    return false;
                }
            }
        }

        if (this.logsFileTitle !=null){
            if (!this.logsFileTitle.equals(anImport.getLogsFileTitle())){
                Pattern pattern = Pattern.compile(this.logsFileTitle);
                if (!pattern.matcher(anImport.getLogsFileTitle()).find()){
                    return false;
                }
            }
        }

        if (this.linkOnLogsFile!=null){
            if (!this.linkOnLogsFile.equals(anImport.getLinkOnLogsFile())){
                Pattern pattern = Pattern.compile(this.linkOnLogsFile);
                if (!pattern.matcher(anImport.getLinkOnLogsFile()).find()){
                    return false;
                }
            }
        }
        if (this.serviceTitle!=null){
            if(!this.serviceTitle.equals(anImport.getServiceTitle())){
                return false;
            }
        }

        if (this.linkOnServicePage!=null){
            if(!this.linkOnServicePage.equals(anImport.getLinkOnServicePage())){
                return false;
            }
        }

        if (this.categories!=null){
            if(!anImport.categories.containsAll(this.getCategories())){
                return false;
            }
        }

            return true;
    }


}
