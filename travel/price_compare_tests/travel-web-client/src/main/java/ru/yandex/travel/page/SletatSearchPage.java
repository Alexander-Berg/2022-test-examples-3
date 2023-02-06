package ru.yandex.travel.page;

import io.qameta.htmlelements.WebPage;
import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.ExtendedList;
import ru.yandex.travel.element.SletatBanner;
import ru.yandex.travel.element.SletatOperatorChecked;
import ru.yandex.travel.element.SletatSearchForm;
import ru.yandex.travel.element.SletatSearchResult;
import ru.yandex.travel.element.SletatSearchStatus;

public interface SletatSearchPage extends WebPage{

    @FindBy("//div[contains(@class,'uis-popup_r2-banner')]")
    SletatBanner banner();

    @FindBy("//div[@id='searchForm']//section[@class='slsf-container']")
    SletatSearchForm searchForm();

    @FindBy("//div[@class='search-status-container']")
    SletatSearchStatus searchStatus();

    @FindBy("//ul[@class='search-result__list']/li[contains(@class, 'search-result__item_short')]")
    ExtendedList<SletatSearchResult> searchResults();

    @FindBy("//ul[@class='blinchik__operator-list']//label[contains(@class, 'uis-checkbox__label_checked')]/../..")
    ExtendedList<SletatOperatorChecked> operatorsList();

}
