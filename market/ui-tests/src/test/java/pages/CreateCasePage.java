package pages;

public class CreateCasePage {

    public static String caseTheme = "//*[contains(text(),'Тема')]/../..//input";
    public static String caseContact = "//*[contains(text(),'Имя контакта')]/../..//input";

    public static String saveButtonInModal = "//footer//*[contains(text(), 'Сохранить')]";
}
