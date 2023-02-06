package ru.yandex.autotests.innerpochta.ns.pages.nonmail;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ChromeFileBrowserPage extends MailPage {

    @Name("Файлы в папке")
    @FindByCss(".icon.file")
    ElementsCollection<MailElement> downloadedFiles();

    @Name("Размер файла")
    default WebElement fileSize(String filename){
        return getWrappedDriver().findElement(By.cssSelector("[data-value='" + filename + "'] + td"));
    }
}
