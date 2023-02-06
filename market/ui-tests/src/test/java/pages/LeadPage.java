package pages;

import init.Config;

public class LeadPage {

    public static String createPage = Config.getProjectURL() + "/lightning/o/Lead/new";

    //Поля со страницы редактирования
    public static String editCompanyField = "//*[@name='Company']";
    public static String editLastNameField = "//*[@name='lastName']";
    public static String editEmailField = "//*[@name='Email']";

    //Статусы со статусбара
    public static String currentStatusOnStatusBar = "//div[@aria-label='Path Header']//a[@tabindex='0']";
    public static String appointedStatusOnStatusBar = "//li[@data-name='Appointed']";
    public static String lostStatusOnStatusBar = "//li[@data-name='Lost']";

    //Статусы с пиклиста
    public static String appointedStatus = "//span[@title='Распределены']";
    public static String defaultReason = "//span[@title='Без объяснения']";
    public static String defaultExtendedReason = "//span[@title='Без объяснения причин']";

    //Пиклисты
    public static String statusPicklist = "//button[starts-with(@aria-label, 'Статус интереса')]";
    public static String lostReasonPicklist = "//button[starts-with(@aria-label, 'Причина отказа')]";
    public static String lostExtendedReasonPicklist = "//button[starts-with(@aria-label, 'Расширенная причина отказа')]";

    //Кнопки
    public static String editStatusButton = "//button[@title='Редактировать: Статус интереса']";
    public static String editLostReasonButton = "//button[@title='Редактировать: Причина отказа']";
    public static String activeSelectedStatusButton = "//button[contains(@class,'stepAction')]";
    public static String createMailCaseButton = "//button[@name='Lead.CreateCase']";

    //Табы
    public static String communicationTab = "//a[@data-label='Коммуникации']";

    //Задача
    public static String taskTypePickList = "//a[@class='select']";
    public static String qualifiedTaskType = "//a[@title='Квалифицировать лид']";
    public static String taskNameField = "//input[@data-value='Задача']";

    //Действия
    public static String actionCommentField = "//span[contains(text(), 'Комментарий')]/../..//textarea";
    public static String saveActionButton = "//section[contains(@class, 'tabs__content active')]//div[contains(@class, 'bottomBarRight')]";

    //Заметки
    public static String noteDescription = "//div[contains(@class, 'description')]";
    public static String noteExpandButton = "//*[@title='Сведения для: Заметка']";

}
