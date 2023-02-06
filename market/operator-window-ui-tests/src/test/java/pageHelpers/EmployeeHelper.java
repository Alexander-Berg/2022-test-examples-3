package ui_tests.src.test.java.pageHelpers;

import Classes.Employee;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import pages.Pages;

import java.util.Set;

public class EmployeeHelper {
    private WebDriver webDriver;

    public EmployeeHelper(WebDriver webDriver){
        this.webDriver=webDriver;
    }
    /**
     * Открыть страницу редактирования, изменить данные и сохранить изменпния
     *
     * @param employee Сотрудник
     */
    public void editEmployee(Employee employee) {
        Pages.employeePage(webDriver).viewRecordPage().mainPropertiesTab().clickEditPropertiesButton();

        if (employee.getAlias() != null) {
            Pages.employeePage(webDriver).modalWindowCreateOrEditRecords().setAlias(employee.getAlias());
        }
        if (employee.getOu() != null) {
            Pages.employeePage(webDriver).modalWindowCreateOrEditRecords().setOU(employee.getOu());
        }
        if (employee.getRoles() != null) {
            Pages.employeePage(webDriver).modalWindowCreateOrEditRecords().setRoles(employee.getRoles());
        }
        if (employee.getServices() != null) {
            Pages.employeePage(webDriver).modalWindowCreateOrEditRecords().setServices(employee.getServices());
        }
        if (employee.getTeams() != null) {
            Pages.employeePage(webDriver).modalWindowCreateOrEditRecords().setTeams(employee.getTeams());
        }
        if (employee.getTitle() != null) {
            Pages.employeePage(webDriver).modalWindowCreateOrEditRecords().setTitle(employee.getTitle());
        }
        Pages.employeePage(webDriver).modalWindowCreateOrEditRecords().clickSaveRecordButton();
    }

    /**
     * Получить все свойства со страницы
     * @return
     */
    public Employee getAllPropertiesFromPage(){
        Employee employee = new Employee();
        employee.setAlias(Pages.employeePage(webDriver).viewRecordPage().mainPropertiesTab().getAlias());
        employee.setOu(Pages.employeePage(webDriver).viewRecordPage().mainPropertiesTab().getOU());
        employee.setRoles(Pages.employeePage(webDriver).viewRecordPage().mainPropertiesTab().getRoles());
        employee.setServices(Pages.employeePage(webDriver).viewRecordPage().mainPropertiesTab().getServices());
        employee.setTeams(Pages.employeePage(webDriver).viewRecordPage().mainPropertiesTab().getTeams());
        employee.setTitle(Pages.employeePage(webDriver).viewRecordPage().mainPropertiesTab().getTitle());
        return employee;
    }

    /**
     * Получить loginStaff из куки браузера
     * @return
     */
    public String getLoginStaffFromCookie(){
        String loginStaff = null;
        Set<Cookie> cookies = webDriver.manage().getCookies();

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("yandex_login")) {
                loginStaff = cookie.getValue();
                break;
            }
        }
        if (loginStaff == null) {
            throw new Error("Не удалось получить из куки значение loginStaff");
        }
        return loginStaff;
    }
}
