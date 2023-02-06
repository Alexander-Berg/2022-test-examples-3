package ui_tests.src.test.java.pageHelpers;

import Classes.ticket.Ticket;
import entity.Entity;
import org.openqa.selenium.WebDriver;
import pages.Pages;
import tools.Tools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;

public class TableHelper {
    private WebDriver webDriver;

    public TableHelper(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Создать новый тикет ручным методом
     *
     * @param ticketType метакласс тикета как в интерфейсе
     * @param newTicket  обращение
     */
    public void createNewTicket(String ticketType, Ticket newTicket) {
        //Нажимаем на кнопку создания обращения
        Entity.entityTable(webDriver).toolBar().clickOnAddTicketButton();
        //Выбираем тип создаваемого обращения
        Entity.entityTable(webDriver).toolBar().selectEntityOnSelectMenu(ticketType);
        Pages.ticketPage(webDriver).toast().hideNotificationError();

        //Заполняем свойства обращения
        Pages.ticketPage(webDriver).createTicketPage().properties()
                .setCategories(newTicket.getProperties().getCategory())
                .setClientEmail(newTicket.getProperties().getContactEmail())
                .setTitle(newTicket.getSubject())
                .setContactPhoneNumber(newTicket.getProperties().getContactPhoneNumber())
                .setDeliveryOrder(newTicket.getProperties().getDeliveryOrder())
                .setOrder(newTicket.getProperties().getOrder());

        if (ticketType.contains("поддержка")) {
            Pages.ticketPage(webDriver).createTicketPage().properties().setService(Collections.singletonList(newTicket.getProperties().getService()));
        } else if (ticketType.contains("исходящее")) {
            Pages.ticketPage(webDriver).createTicketPage().properties().setServiceSelect(newTicket.getProperties().getService());
        } else if (ticketType.contains("исходящий звонок")) {
            Pages.ticketPage(webDriver).createTicketPage().properties().setServiceSuggest(newTicket.getProperties().getService());
        }
        if (newTicket.getComments() != null && newTicket.getComments().size() > 0) {
            PageHelper.createTicketPageHelper(webDriver).setComment(newTicket.getComments().get(0));
        }
        //Нажимаем на сохранение обращения
        Pages.ticketPage(webDriver).createTicketPage().header().clickButtonSaveForm("Добавить");
    }

    /**
     * Открыть вкладку
     *
     * @param tabName имя вкладки
     */
    public void openTab(String tabName) {
        Entity
                .tabs(webDriver)
                .openTab(tabName);
    }

    /**
     * Применить предсохраненный фильтр
     *
     * @param filterName
     */
    public void setSavedFilter(String filterName) {
        Entity.entityTable(webDriver).toolBar().setSavedFilter(filterName);
    }

    public void openTicketPage(String subjectTicket) {
        openTicketPage(subjectTicket, 9);
    }

    public void openTicketPage(String subjectTicket, int countOfAttempts) {
        int x = 0;

        do {
            if (x != 0) {
                Tools.waitElement(webDriver).waitTime(15000);
            }
            quicklyFindEntity(subjectTicket);
            try {
                Entity.entityTable(webDriver).content().openEntity(subjectTicket);
                return;
            } catch (Throwable e) {
                if (e.toString().contains("Не удалось открыть страницу с названием")) {
                    // Entity.entityTab().footer().nextPageButtonClick();
                    x++;
                } else {
                    throw new Error(e);
                }
            }
        } while (x < countOfAttempts);

        throw new Error("Не нашлось обращение с темой " + subjectTicket);
    }

    /**
     * Открыть рандомный объект из таблицы
     */
    public void openRandomEntity() {
        Pages.ticketPage(webDriver).toast().hideNotificationError();
        Entity.entityTable(webDriver).content().openRandomEntity();
    }

    /**
     * Найти элемент через быстрый поиск
     *
     * @param title - заголовок сервера для поиска
     * @return
     */
    public void quicklyFindEntity(String title) {
        Entity.entityTable(webDriver).toolBar().clearQuickSearch();
        Entity.entityTable(webDriver).toolBar().setQuickSearch(title);
        Entity.entityTable(webDriver).toolBar().quickSearchButtonClick();
    }

    /**
     * Отобразить архивные записи за последние 8 месяцев
     */
    public void showArchivedRecords() {
        Entity
                .entityTable(webDriver)
                .toolBar()
                .displayArchivalEntityButtonClick();
        DateFormat formater = new SimpleDateFormat("ddMMyyyy");
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.set(Calendar.MONTH, new GregorianCalendar().get(Calendar.MONTH) - 8);

        Entity.entityTable(webDriver).toolBar().setStartTimeArchived(formater.format(gregorianCalendar.getTime()));
        Entity.entityTable(webDriver).toolBar().applyingArchiveTimeFilter();
    }
}
