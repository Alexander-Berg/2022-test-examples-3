package ru.yandex.autotests.innerpochta.util;

import com.google.common.collect.Sets;
import org.openqa.selenium.By;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.util.Set;

import static org.openqa.selenium.By.cssSelector;

/**
 * @author cosmopanda
 */
public class TestConsts {

    public static final Set<By> IGNORED_ELEMENTS = Sets.newHashSet(
        cssSelector(".ns-view-footer"),
        cssSelector(".ns-view-collectors"),
        cssSelector(".mail-User-Picture"),
        cssSelector(".js-layout-left-toggler"),
        cssSelector(".ns-view-compose-autosave-status"),
        cssSelector(".mail-NestedList-Item-Info"),
        cssSelector(".b-captcha__wrapper"),
        cssSelector(".b-account-activity__current-ip__ip"),
        cssSelector(".js-header-left-column"),
        cssSelector(".mail-SignatureChooser"),
        cssSelector(".ns-view-messages-filters-unread"),
        cssSelector(".mail-User-Avatar"),
        cssSelector(".mail-Compose-Field-Misc"),
        cssSelector(".ns-view-head-user"),
        cssSelector(".fid-6"),
        cssSelector(".js-messages-pager-scroll"),
        cssSelector(".ComposeStack"),
        cssSelector(".composeHeader-SavedAt"),
        cssSelector(".mail-LoadingBar-Container")
    );

    public static final Set<Coords> IGNORED_AREAS = Sets.newHashSet(
        new Coords(0, 0, 1920, 5)  // скачет полоска лоадера
    );

    //Поменять координаты при изменении разрешения (сейчас 1920x1080)
    public static final Set<Coords> REFRESH_BUTTON_PIXELS = Sets.newHashSet(
        new Coords(261, 85, 1, 1),
        new Coords(262, 86, 1, 1),
        new Coords(293, 86, 1, 1),
        new Coords(294, 85, 1, 1)
    );

    public static final String HTML = "<!doctype html><html><head> <meta http-equiv=\"Content-Type\" " +
        "content=\"text/html; " +
        "charset=UTF-8\"/> <title></title> <style>.mail-address a, .mail-address a[href]{text-decoration: none " +
        "!important; color: #000000 !important;}</style></head><body> <table cellpadding=\"0\" cellspacing=\"0\" " +
        "align=\"center\" width=\"770px\" style=\"font-family: Arial, sans-serif; color: #000000; " +
        "background-color: #f8f8f8; font-size: 14px; background-image: url('https://yastatic" +
        ".net/passport/_/VuEUwcSoZovM7QELisjTHCYkENk.png') repeat;\"> <tr> <td style=\"padding-top: 60px; " +
        "padding-right: 70px; padding-bottom: 60px; padding-left: 70px;\"> <img src=\"https://yastatic" +
        ".net/passport/_/hs6ZkXRRblJu-udsCbQehxGk29A.png\" alt=\"\" style=\"margin-left: 30px; margin-bottom: " +
        "15px;\"> <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" align=\"center\" " +
        "style=\"border-color: #e6e6e6; border-width: 1px; border-style: solid; background-color: #fff; " +
        "padding-top: 25px; padding-right: 0; padding-bottom: 50px; padding-left: 30px;\"> <tr> <td " +
        "style=\"padding: 0 30px 30px;\"> <p style=\"font-family: Arial, sans-serif; color: #000000; font-size: " +
        "19px; margin-top: 14px; margin-bottom: 0;\"> Здравствуйте, длинное длинное имя пользователя-тест-тест " +
        "тест ?*! </p><p style=\"font-family: Arial, sans-serif; color: #000000; font-size: 14px; line-height: " +
        "17px; margin-top: 30px; margin-bottom: 30px;\"> Вы отозвали пароли приложений, по которым сторонние " +
        "программы получали доступ к Вашим данным на Яндексе. </p><p style=\"font-family: Arial, sans-serif; " +
        "color: #000000; font-size: 14px; line-height: 17px; margin-top: 0; margin-bottom: 30px;\"> Чтобы эти " +
        "программы снова могли подключаться к Вашему аккаунту, получите для них новые <a href='https://passport" +
        ".yandex.ru/profile/access'>пароли приложений</a> или используйте свой обычный пароль. </p><p " +
        "style=\"font-family: Arial, sans-serif; color: #000000; font-size: 14px; line-height: 17px; margin-top: " +
        "0; margin-bottom: 30px;\"> Вы также можете включить дополнительную защиту своего аккаунта — " +
        "двухфакторную аутентификацию. В этом случае предоставлять доступ сторонним программам можно будет только" +
        " по паролям приложений. </p><p style=\"font-family: Arial, sans-serif; color: #000000; font-size: 15px; " +
        "font-style: italic; margin-top: 30px; margin-bottom: 0;\"> С заботой о безопасности Вашего аккаунта," +
        "<br>команда Яндекс.Паспорта </p></td></tr></table> <table width=\"100%\" cellpadding=\"0\" " +
        "cellspacing=\"0\" align=\"center\"> <tr> <td style=\"padding-top: 12px; background-image: url" +
        "('https://yastatic.net/passport/_/GiCv5zMnyvmSWSkhAJff47-h7pk.png');\"></td></tr><tr> <td " +
        "style=\"font-family: Arial, sans-serif; font-size: 12px; color: #888888; padding-right: 30px; " +
        "padding-left: 30px;\"> Пожалуйста, не отвечайте на это письмо. Связаться со службой поддержки Яндекса Вы" +
        " можете через <a href='https://feedback2.yandex.ru/'>форму обратной связи</a>. </td></tr></table> " +
        "</td></tr></table></body></html>";

}
