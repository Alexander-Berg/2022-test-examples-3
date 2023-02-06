package ru.yandex.autotests.direct.cmd.data.stat.report;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.excelstatistic.ReportFileFormat;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatTypeEnum;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ShowStatRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private String cid;

    @SerializeKey("show_stat")
    private String showStat;

    @SerializeKey("stat_periods")
    private String statPeriods;

    @SerializeKey("group_by_date")
    private String groupByDate;

    @SerializeKey("page_size")
    private String pageSize;

    @SerializeKey("date_from")
    private String dateFrom;

    @SerializeKey("date_to")
    private String dateTo;

    @SerializeKey("with_nds")
    private String withNds;

    @SerializeKey("with_discount")
    private String withDiscount;

    @SerializeKey("group_by")
    private String groupBy;

    @SerializeKey("group_by_positions")
    private String groupByPositions;

    @SerializeKey("format")
    private ReportFileFormat fileFormat;

    @SerializeKey("stat_type")
    private StatTypeEnum statType;

    @SerializeKey("columns")
    private String columns;

    @SerializeKey("columns_positions")
    private String columnsPosition;

    @SerializeKey("single_camp")
    private String singleCamp;

    /* Фильтры */

    //Кампании - тип
    @SerializeKey("fl_campaign_type__eq")
    private String flCampaignTypeEq;

    //Кампании - № / название (равно)
    @SerializeKey("fl_campaign__eq")
    private String flCampaignEq;

    //Кампании - № / название (не равно)
    @SerializeKey("fl_campaign__ne")
    private String flCampaignNe;

    //Кампании - стратегия
    @SerializeKey("fl_campaign_strategy__eq")
    private String flCampaignStrategyEq;

    //Кампании - статус
    @SerializeKey("fl_campaign_status__eq")
    private String flCampaignStatusEq;


    //Метки


    //Группы - № / название (равно)
    @SerializeKey("fl_adgroup__starts_with")
    private String flAdgroupStartsWith;

    //Группы - № / название (равно, точное соответствие)
    @SerializeKey("fl_adgroup__eq")
    private String flAdgroupEq;

    //Группы - № / название (не равно)
    @SerializeKey("fl_adgroup__not_starts_with")
    private String flAdgroupNotStartsWith;

    //Группы - № / название (не равно, точное соответствие)
    @SerializeKey("fl_adgroup__ne")
    private String flAdgroupNe;

    //Группы - статус
    @SerializeKey("fl_adgroup_status__eq")
    private String flAdgroupStatusEq;


    //Объявления - № (равно)
    @SerializeKey("fl_banner__eq")
    private String flBannerEq;

    //Объявления - № (не равно)
    @SerializeKey("fl_banner__ne")
    private String flBannerNe;

    //Объявления - cтатус
    @SerializeKey("fl_banner_status__eq")
    private String flBannerStatusEq;


    //Тип условия показа
    @SerializeKey("fl_contexttype__eq")
    private String flContexttypeEq;


    //Текст фразы (равно)
    @SerializeKey("fl_phrase__starts_with")
    private String flPhraseStartsWith;

    //Текст фразы (равно, точное соответствие)
    @SerializeKey("fl_phrase__eq")
    private String flPhraseEq;

    //Текст фразы (не равно)
    @SerializeKey("fl_phrase__not_starts_with")
    private String flPhraseNotStartsWith;

    //Текст фразы (не равно, точное соответствие)
    @SerializeKey("fl_phrase__ne")
    private String flPhraseNe;


    //Условия ретаргетинга

    //Условия нацеливания


    //Регионы
    @SerializeKey("fl_region__eq")
    private String flRegionEq;


    //Площадки - тип
    @SerializeKey("fl_targettype__eq")
    private String flTargettypeEq;

    //Площадки - название (равно)
    @SerializeKey("fl_page__eq")
    private String flPageEq;

    //Площадки - название (не равно)
    @SerializeKey("fl_page__ne")
    private String flPageNe;

    //Позиция
    @SerializeKey("fl_position__eq")
    private String flPositionEq;

    //Место клика
    @SerializeKey("fl_click_place__eq")
    private String flClickPlaceEq;


    //Изображение
    @SerializeKey("fl_has_image__eq")
    private String flHasImageEq;

    //Тип устройств
    @SerializeKey("fl_device_type__eq")
    private String flDeviceTypeEq;

    //Тип операционной системы
    @SerializeKey("fl_detailed_device_type__eq")
    private String flDetailedDeviceTypeEq;

    //Тип связи
    @SerializeKey("fl_connection_type__eq")
    private String flConnectionTypeEq;

    //Пол
    @SerializeKey("fl_gender__eq")
    private String flGenderEq;

    //Возраст
    @SerializeKey("fl_age__eq")
    private String flAgeEq;

    //Цель



    //Показы (больше)
    @SerializeKey("fl_shows__gt")
    private String flShowsGt;

    //Показы (меньше)
    @SerializeKey("fl_shows__lt")
    private String flShowsLt;

    //Показы (равно)
    @SerializeKey("fl_shows__eq")
    private String flShowsEq;

    //Клики (больше)
    @SerializeKey("fl_clicks__gt")
    private String flClicksGt;

    //Клики (меньше)
    @SerializeKey("fl_clicks__lt")
    private String flClicksLt;

    //Клики (равно)
    @SerializeKey("fl_clicks__eq")
    private String flClicksEq;

    //CTR (%) (больше)
    @SerializeKey("fl_ctr__gt")
    private String flCtrGt;

    //CTR (%) (меньше)
    @SerializeKey("fl_ctr__lt")
    private String flCtrLt;

    //CTR (%) (равно)
    @SerializeKey("fl_ctr__eq")
    private String flCtrEq;

    //Расход всего (больше)
    @SerializeKey("fl_sum__gt")
    private String flSumGt;

    //Расход всего (меньше)
    @SerializeKey("fl_sum__lt")
    private String flSumLt;

    //Расход всего (равно)
    @SerializeKey("fl_sum__eq")
    private String flSumEq;

    //Ср. цена клика (больше)
    @SerializeKey("fl_av_sum__gt")
    private String flAvSumGt;

    //Ср. цена клика (меньше)
    @SerializeKey("fl_av_sum__lt")
    private String flAvSumLt;

    //Ср. цена клика (равно)
    @SerializeKey("fl_av_sum__eq")
    private String flAvSumEq;

    //Ср. позиция показа (больше)
    @SerializeKey("fl_fp_shows_avg_pos__gt")
    private String flFpShowsAvgPosGt;

    //Ср. позиция показа (меньше)
    @SerializeKey("fl_fp_shows_avg_pos__lt")
    private String flFpShowsAvgPosLt;

    //Ср. позиция показа (равно)
    @SerializeKey("fl_fp_shows_avg_pos__eq")
    private String flFpShowsAvgPosEq;

    //Ср. позиция клика (больше)
    @SerializeKey("fl_fp_clicks_avg_pos__gt")
    private String flFpClicksAvgPosGt;

    //Ср. позиция клика (меньше)
    @SerializeKey("fl_fp_clicks_avg_pos__lt")
    private String flFpClicksAvgPosLt;

    //Ср. позиция клика (равно)
    @SerializeKey("fl_fp_clicks_avg_pos__eq")
    private String flFpClicksAvgPosEq;

    //Глубина (стр.) (больше)
    @SerializeKey("fl_adepth__gt")
    private String flAdepthGt;

    //Глубина (стр.) (меньше)
    @SerializeKey("fl_adepth__lt")
    private String flAdepthLt;

    //Глубина (стр.) (равно)
    @SerializeKey("fl_adepth__eq")
    private String flAdepthEq;

    //Конверсия (%) (больше)
    @SerializeKey("fl_aconv__gt")
    private String flAconvGt;

    //Конверсия (%) (меньше)
    @SerializeKey("fl_aconv__lt")
    private String flAconvLt;

    //Конверсия (%) (равно)
    @SerializeKey("fl_aconv__eq")
    private String flAconvEq;

    //Количество конверсий (больше)
    @SerializeKey("fl_agoalnum__gt")
    private String flAgoalnumGt;

    //Количество конверсий (меньше)
    @SerializeKey("fl_agoalnum__lt")
    private String flAgoalnumLt;

    //Количество конверсий (равно)
    @SerializeKey("fl_agoalnum__eq")
    private String flAgoalnumEq;

    //Цена цели (больше)
    @SerializeKey("fl_agoalcost__gt")
    private String flAgoalcostGt;

    //Цена цели (меньше)
    @SerializeKey("fl_agoalcost__lt")
    private String flAgoalcostLt;

    //Цена цели (равно)
    @SerializeKey("fl_agoalcost__eq")
    private String flAgoalcostEq;

    //Рентабельность (больше)
    @SerializeKey("fl_agoalroi__gt")
    private String flAgoalroiGt;

    //Рентабельность (меньше)
    @SerializeKey("fl_agoalroi__lt")
    private String flAgoalroiLt;

    //Рентабельность (равно)
    @SerializeKey("fl_agoalroi__eq")
    private String flAgoalroiEq;

    //Доход (больше)
    @SerializeKey("fl_agoalincome__gt")
    private String flAgoalincomeGt;

    //Доход (меньше)
    @SerializeKey("fl_agoalincome__lt")
    private String flAgoalincomeLt;

    //Доход (равно)
    @SerializeKey("fl_agoalincome__eq")
    private String flAgoalincomeEq;




    public String getShowStat() {
        return showStat;
    }

    public void setShowStat(String showStat) {
        this.showStat = showStat;
    }

    public String getStatPeriods() {
        return statPeriods;
    }

    public void setStatPeriods(String statPeriods) {
        this.statPeriods = statPeriods;
    }

    public String getGroupByDate() {
        return groupByDate;
    }

    public void setGroupByDate(String groupByDate) {
        this.groupByDate = groupByDate;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public String getWithNds() {
        return withNds;
    }

    public void setWithNds(String withNds) {
        this.withNds = withNds;
    }

    public String getWithDiscount() {
        return withDiscount;
    }

    public void setWithDiscount(String withDiscount) {
        this.withDiscount = withDiscount;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public String getGroupByPositions() {
        return groupByPositions;
    }

    public void setGroupByPositions(String groupByPositions) {
        this.groupByPositions = groupByPositions;
    }

    public String getFlPhraseStartsWith() {
        return flPhraseStartsWith;
    }

    public void setFlPhraseStartsWith(String flPhraseStartsWith) {
        this.flPhraseStartsWith = flPhraseStartsWith;
    }

    public String getFlCampaignTypeEq() {
        return flCampaignTypeEq;
    }

    public void setFlCampaignTypeEq(String flCampaignTypeEq) {
        this.flCampaignTypeEq = flCampaignTypeEq;
    }

    public String getFlCampaignEq() {
        return flCampaignEq;
    }

    public void setFlCampaignEq(String flCampaignEq) {
        this.flCampaignEq = flCampaignEq;
    }

    public String getFlCampaignNe() {
        return flCampaignNe;
    }

    public void setFlCampaignNe(String flCampaignNe) {
        this.flCampaignNe = flCampaignNe;
    }

    public String getFlCampaignStrategyEq() {
        return flCampaignStrategyEq;
    }

    public void setFlCampaignStrategyEq(String flCampaignStrategyEq) {
        this.flCampaignStrategyEq = flCampaignStrategyEq;
    }

    public String getFlCampaignStatusEq() {
        return flCampaignStatusEq;
    }

    public void setFlCampaignStatusEq(String flCampaignStatusEq) {
        this.flCampaignStatusEq = flCampaignStatusEq;
    }

    public String getFlAdgroupStartsWith() {
        return flAdgroupStartsWith;
    }

    public void setFlAdgroupStartsWith(String flAdgroupStartsWith) {
        this.flAdgroupStartsWith = flAdgroupStartsWith;
    }

    public String getFlAdgroupEq() {
        return flAdgroupEq;
    }

    public void setFlAdgroupEq(String flAdgroupEq) {
        this.flAdgroupEq = flAdgroupEq;
    }

    public String getFlAdgroupNotStartsWith() {
        return flAdgroupNotStartsWith;
    }

    public void setFlAdgroupNotStartsWith(String flAdgroupNotStartsWith) {
        this.flAdgroupNotStartsWith = flAdgroupNotStartsWith;
    }

    public String getFlAdgroupNe() {
        return flAdgroupNe;
    }

    public void setFlAdgroupNe(String flAdgroupNe) {
        this.flAdgroupNe = flAdgroupNe;
    }

    public String getFlAdgroupStatusEq() {
        return flAdgroupStatusEq;
    }

    public void setFlAdgroupStatusEq(String flAdgroupStatusEq) {
        this.flAdgroupStatusEq = flAdgroupStatusEq;
    }

    public String getFlBannerEq() {
        return flBannerEq;
    }

    public void setFlBannerEq(String flBannerEq) {
        this.flBannerEq = flBannerEq;
    }

    public String getFlBannerNe() {
        return flBannerNe;
    }

    public void setFlBannerNe(String flBannerNe) {
        this.flBannerNe = flBannerNe;
    }

    public String getFlBannerStatusEq() {
        return flBannerStatusEq;
    }

    public void setFlBannerStatusEq(String flBannerStatusEq) {
        this.flBannerStatusEq = flBannerStatusEq;
    }

    public String getFlContexttypeEq() {
        return flContexttypeEq;
    }

    public void setFlContexttypeEq(String flContexttypeEq) {
        this.flContexttypeEq = flContexttypeEq;
    }

    public String getFlPhraseEq() {
        return flPhraseEq;
    }

    public void setFlPhraseEq(String flPhraseEq) {
        this.flPhraseEq = flPhraseEq;
    }

    public String getFlPhraseNotStartsWith() {
        return flPhraseNotStartsWith;
    }

    public void setFlPhraseNotStartsWith(String flPhraseNotStartsWith) {
        this.flPhraseNotStartsWith = flPhraseNotStartsWith;
    }

    public String getFlPhraseNe() {
        return flPhraseNe;
    }

    public void setFlPhraseNe(String flPhraseNe) {
        this.flPhraseNe = flPhraseNe;
    }

    public String getFlRegionEq() {
        return flRegionEq;
    }

    public void setFlRegionEq(String flRegionEq) {
        this.flRegionEq = flRegionEq;
    }

    public String getFlTargettypeEq() {
        return flTargettypeEq;
    }

    public void setFlTargettypeEq(String flTargettypeEq) {
        this.flTargettypeEq = flTargettypeEq;
    }

    public String getFlPageEq() {
        return flPageEq;
    }

    public void setFlPageEq(String flPageEq) {
        this.flPageEq = flPageEq;
    }

    public String getFlPageNe() {
        return flPageNe;
    }

    public void setFlPageNe(String flPageNe) {
        this.flPageNe = flPageNe;
    }

    public String getFlPositionEq() {
        return flPositionEq;
    }

    public void setFlPositionEq(String flPositionEq) {
        this.flPositionEq = flPositionEq;
    }

    public String getFlClickPlaceEq() {
        return flClickPlaceEq;
    }

    public void setFlClickPlaceEq(String flClickPlaceEq) {
        this.flClickPlaceEq = flClickPlaceEq;
    }

    public String getFlHasImageEq() {
        return flHasImageEq;
    }

    public void setFlHasImageEq(String flHasImageEq) {
        this.flHasImageEq = flHasImageEq;
    }

    public String getFlDeviceTypeEq() {
        return flDeviceTypeEq;
    }

    public void setFlDeviceTypeEq(String flDeviceTypeEq) {
        this.flDeviceTypeEq = flDeviceTypeEq;
    }

    public String getFlDetailedDeviceTypeEq() {
        return flDetailedDeviceTypeEq;
    }

    public void setFlDetailedDeviceTypeEq(String flDetailedDeviceTypeEq) {
        this.flDetailedDeviceTypeEq = flDetailedDeviceTypeEq;
    }

    public String getFlConnectionTypeEq() {
        return flConnectionTypeEq;
    }

    public void setFlConnectionTypeEq(String flConnectionTypeEq) {
        this.flConnectionTypeEq = flConnectionTypeEq;
    }

    public String getFlGenderEq() {
        return flGenderEq;
    }

    public void setFlGenderEq(String flGenderEq) {
        this.flGenderEq = flGenderEq;
    }

    public String getFlAgeEq() {
        return flAgeEq;
    }

    public void setFlAgeEq(String flAgeEq) {
        this.flAgeEq = flAgeEq;
    }

    public String getFlShowsGt() {
        return flShowsGt;
    }

    public void setFlShowsGt(String flShowsGt) {
        this.flShowsGt = flShowsGt;
    }

    public String getFlShowsLt() {
        return flShowsLt;
    }

    public void setFlShowsLt(String flShowsLt) {
        this.flShowsLt = flShowsLt;
    }

    public String getFlShowsEq() {
        return flShowsEq;
    }

    public void setFlShowsEq(String flShowsEq) {
        this.flShowsEq = flShowsEq;
    }

    public String getFlClicksGt() {
        return flClicksGt;
    }

    public void setFlClicksGt(String flClicksGt) {
        this.flClicksGt = flClicksGt;
    }

    public String getFlClicksLt() {
        return flClicksLt;
    }

    public void setFlClicksLt(String flClicksLt) {
        this.flClicksLt = flClicksLt;
    }

    public String getFlClicksEq() {
        return flClicksEq;
    }

    public void setFlClicksEq(String flClicksEq) {
        this.flClicksEq = flClicksEq;
    }

    public String getFlCtrGt() {
        return flCtrGt;
    }

    public void setFlCtrGt(String flCtrGt) {
        this.flCtrGt = flCtrGt;
    }

    public String getFlCtrLt() {
        return flCtrLt;
    }

    public void setFlCtrLt(String flCtrLt) {
        this.flCtrLt = flCtrLt;
    }

    public String getFlCtrEq() {
        return flCtrEq;
    }

    public void setFlCtrEq(String flCtrEq) {
        this.flCtrEq = flCtrEq;
    }

    public String getFlSumGt() {
        return flSumGt;
    }

    public void setFlSumGt(String flSumGt) {
        this.flSumGt = flSumGt;
    }

    public String getFlSumLt() {
        return flSumLt;
    }

    public void setFlSumLt(String flSumLt) {
        this.flSumLt = flSumLt;
    }

    public String getFlSumEq() {
        return flSumEq;
    }

    public void setFlSumEq(String flSumEq) {
        this.flSumEq = flSumEq;
    }

    public String getFlAvSumGt() {
        return flAvSumGt;
    }

    public void setFlAvSumGt(String flAvSumGt) {
        this.flAvSumGt = flAvSumGt;
    }

    public String getFlAvSumLt() {
        return flAvSumLt;
    }

    public void setFlAvSumLt(String flAvSumLt) {
        this.flAvSumLt = flAvSumLt;
    }

    public String getFlAvSumEq() {
        return flAvSumEq;
    }

    public void setFlAvSumEq(String flAvSumEq) {
        this.flAvSumEq = flAvSumEq;
    }

    public String getFlFpShowsAvgPosGt() {
        return flFpShowsAvgPosGt;
    }

    public void setFlFpShowsAvgPosGt(String flFpShowsAvgPosGt) {
        this.flFpShowsAvgPosGt = flFpShowsAvgPosGt;
    }

    public String getFlFpShowsAvgPosLt() {
        return flFpShowsAvgPosLt;
    }

    public void setFlFpShowsAvgPosLt(String flFpShowsAvgPosLt) {
        this.flFpShowsAvgPosLt = flFpShowsAvgPosLt;
    }

    public String getFlFpShowsAvgPosEq() {
        return flFpShowsAvgPosEq;
    }

    public void setFlFpShowsAvgPosEq(String flFpShowsAvgPosEq) {
        this.flFpShowsAvgPosEq = flFpShowsAvgPosEq;
    }

    public String getFlFpClicksAvgPosGt() {
        return flFpClicksAvgPosGt;
    }

    public void setFlFpClicksAvgPosGt(String flFpClicksAvgPosGt) {
        this.flFpClicksAvgPosGt = flFpClicksAvgPosGt;
    }

    public String getFlFpClicksAvgPosLt() {
        return flFpClicksAvgPosLt;
    }

    public void setFlFpClicksAvgPosLt(String flFpClicksAvgPosLt) {
        this.flFpClicksAvgPosLt = flFpClicksAvgPosLt;
    }

    public String getFlFpClicksAvgPosEq() {
        return flFpClicksAvgPosEq;
    }

    public void setFlFpClicksAvgPosEq(String flFpClicksAvgPosEq) {
        this.flFpClicksAvgPosEq = flFpClicksAvgPosEq;
    }

    public String getFlAdepthGt() {
        return flAdepthGt;
    }

    public void setFlAdepthGt(String flAdepthGt) {
        this.flAdepthGt = flAdepthGt;
    }

    public String getFlAdepthLt() {
        return flAdepthLt;
    }

    public void setFlAdepthLt(String flAdepthLt) {
        this.flAdepthLt = flAdepthLt;
    }

    public String getFlAdepthEq() {
        return flAdepthEq;
    }

    public void setFlAdepthEq(String flAdepthEq) {
        this.flAdepthEq = flAdepthEq;
    }

    public String getFlAconvGt() {
        return flAconvGt;
    }

    public void setFlAconvGt(String flAconvGt) {
        this.flAconvGt = flAconvGt;
    }

    public String getFlAconvLt() {
        return flAconvLt;
    }

    public void setFlAconvLt(String flAconvLt) {
        this.flAconvLt = flAconvLt;
    }

    public String getFlAconvEq() {
        return flAconvEq;
    }

    public void setFlAconvEq(String flAconvEq) {
        this.flAconvEq = flAconvEq;
    }

    public String getFlAgoalnumGt() {
        return flAgoalnumGt;
    }

    public void setFlAgoalnumGt(String flAgoalnumGt) {
        this.flAgoalnumGt = flAgoalnumGt;
    }

    public String getFlAgoalnumLt() {
        return flAgoalnumLt;
    }

    public void setFlAgoalnumLt(String flAgoalnumLt) {
        this.flAgoalnumLt = flAgoalnumLt;
    }

    public String getFlAgoalnumEq() {
        return flAgoalnumEq;
    }

    public void setFlAgoalnumEq(String flAgoalnumEq) {
        this.flAgoalnumEq = flAgoalnumEq;
    }

    public String getFlAgoalcostGt() {
        return flAgoalcostGt;
    }

    public void setFlAgoalcostGt(String flAgoalcostGt) {
        this.flAgoalcostGt = flAgoalcostGt;
    }

    public String getFlAgoalcostLt() {
        return flAgoalcostLt;
    }

    public void setFlAgoalcostLt(String flAgoalcostLt) {
        this.flAgoalcostLt = flAgoalcostLt;
    }

    public String getFlAgoalcostEq() {
        return flAgoalcostEq;
    }

    public void setFlAgoalcostEq(String flAgoalcostEq) {
        this.flAgoalcostEq = flAgoalcostEq;
    }

    public String getFlAgoalroiGt() {
        return flAgoalroiGt;
    }

    public void setFlAgoalroiGt(String flAgoalroiGt) {
        this.flAgoalroiGt = flAgoalroiGt;
    }

    public String getFlAgoalroiLt() {
        return flAgoalroiLt;
    }

    public void setFlAgoalroiLt(String flAgoalroiLt) {
        this.flAgoalroiLt = flAgoalroiLt;
    }

    public String getFlAgoalroiEq() {
        return flAgoalroiEq;
    }

    public void setFlAgoalroiEq(String flAgoalroiEq) {
        this.flAgoalroiEq = flAgoalroiEq;
    }

    public String getFlAgoalincomeGt() {
        return flAgoalincomeGt;
    }

    public void setFlAgoalincomeGt(String flAgoalincomeGt) {
        this.flAgoalincomeGt = flAgoalincomeGt;
    }

    public String getFlAgoalincomeLt() {
        return flAgoalincomeLt;
    }

    public void setFlAgoalincomeLt(String flAgoalincomeLt) {
        this.flAgoalincomeLt = flAgoalincomeLt;
    }

    public String getFlAgoalincomeEq() {
        return flAgoalincomeEq;
    }

    public void setFlAgoalincomeEq(String flAgoalincomeEq) {
        this.flAgoalincomeEq = flAgoalincomeEq;
    }

    public ReportFileFormat getFileFormat() {
        return fileFormat;
    }

    public StatTypeEnum getStatType() {
        return statType;
    }

    public ShowStatRequest withShowStat(String showStat) {
        this.showStat = showStat;
        return this;
    }

    public ShowStatRequest withStatPeriods(String statPeriods) {
        this.statPeriods = statPeriods;
        return this;
    }

    public ShowStatRequest withGroupByDate(String groupByDate) {
        this.groupByDate = groupByDate;
        return this;
    }

    public ShowStatRequest withPageSize(String pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public ShowStatRequest withDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
        return this;
    }

    public ShowStatRequest withDateTo(String dateTo) {
        this.dateTo = dateTo;
        return this;
    }

    public ShowStatRequest withWithNds(String withNds) {
        this.withNds = withNds;
        return this;
    }

    public ShowStatRequest withWithDiscount(String withDiscount) {
        this.withDiscount = withDiscount;
        return this;
    }

    public ShowStatRequest withGroupBy(String groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    public ShowStatRequest withGroupByPositions(String groupByPositions) {
        this.groupByPositions = groupByPositions;
        return this;
    }

    public ShowStatRequest withFileFormat(ReportFileFormat fileFormat)
    {
        this.fileFormat = fileFormat;
        return this;
    }

    public ShowStatRequest withFlCampaignTypeEq(String flCampaignTypeEq) {
        this.flCampaignTypeEq = flCampaignTypeEq;
        return this;
    }

    public ShowStatRequest withFlCampaignEq(String flCampaignEq) {
        this.flCampaignEq = flCampaignEq;
        return this;
    }

    public ShowStatRequest withFlCampaignNe(String flCampaignNe) {
        this.flCampaignNe = flCampaignNe;
        return this;
    }

    public ShowStatRequest withFlCampaignStrategyEq(String flCampaignStrategyEq) {
        this.flCampaignStrategyEq = flCampaignStrategyEq;
        return this;
    }

    public ShowStatRequest withFlCampaignStatusEq(String flCampaignStatusEq) {
        this.flCampaignStatusEq = flCampaignStatusEq;
        return this;
    }

    public ShowStatRequest withFlAdgroupStartsWith(String flAdgroupStartsWith) {
        this.flAdgroupStartsWith = flAdgroupStartsWith;
        return this;
    }

    public ShowStatRequest withFlAdgroupEq(String flAdgroupEq) {
        this.flAdgroupEq = flAdgroupEq;
        return this;
    }

    public ShowStatRequest withFlAdgroupNotStartsWith(String flAdgroupNotStartsWith) {
        this.flAdgroupNotStartsWith = flAdgroupNotStartsWith;
        return this;
    }

    public ShowStatRequest withFlAdgroupNe(String flAdgroupNe) {
        this.flAdgroupNe = flAdgroupNe;
        return this;
    }

    public ShowStatRequest withFlAdgroupStatusEq(String flAdgroupStatusEq) {
        this.flAdgroupStatusEq = flAdgroupStatusEq;
        return this;
    }

    public ShowStatRequest withFlBannerEq(String flBannerEq) {
        this.flBannerEq = flBannerEq;
        return this;
    }

    public ShowStatRequest withFlBannerNe(String flBannerNe) {
        this.flBannerNe = flBannerNe;
        return this;
    }

    public ShowStatRequest withFlBannerStatusEq(String flBannerStatusEq) {
        this.flBannerStatusEq = flBannerStatusEq;
        return this;
    }

    public ShowStatRequest withFlContexttypeEq(String flContexttypeEq) {
        this.flContexttypeEq = flContexttypeEq;
        return this;
    }

    public ShowStatRequest withFlPhraseStartsWith(String flPhraseStartsWith) {
        this.flPhraseStartsWith = flPhraseStartsWith;
        return this;
    }

    public ShowStatRequest withFlPhraseEq(String flPhraseEq) {
        this.flPhraseEq = flPhraseEq;
        return this;
    }

    public ShowStatRequest withFlPhraseNotStartsWith(String flPhraseNotStartsWith) {
        this.flPhraseNotStartsWith = flPhraseNotStartsWith;
        return this;
    }

    public ShowStatRequest withFlPhraseNe(String flPhraseNe) {
        this.flPhraseNe = flPhraseNe;
        return this;
    }

    public ShowStatRequest withFlRegionEq(String flRegionEq) {
        this.flRegionEq = flRegionEq;
        return this;
    }

    public ShowStatRequest withFlTargettypeEq(String flTargettypeEq) {
        this.flTargettypeEq = flTargettypeEq;
        return this;
    }

    public ShowStatRequest withFlPageEq(String flPageEq) {
        this.flPageEq = flPageEq;
        return this;
    }

    public ShowStatRequest withFlPageNe(String flPageNe) {
        this.flPageNe = flPageNe;
        return this;
    }

    public ShowStatRequest withFlPositionEq(String flPositionEq) {
        this.flPositionEq = flPositionEq;
        return this;
    }

    public ShowStatRequest withFlClickPlaceEq(String flClickPlaceEq) {
        this.flClickPlaceEq = flClickPlaceEq;
        return this;
    }

    public ShowStatRequest withFlHasImageEq(String flHasImageEq) {
        this.flHasImageEq = flHasImageEq;
        return this;
    }

    public ShowStatRequest withFlDeviceTypeEq(String flDeviceTypeEq) {
        this.flDeviceTypeEq = flDeviceTypeEq;
        return this;
    }

    public ShowStatRequest withFlDetailedDeviceTypeEq(String flDetailedDeviceTypeEq) {
        this.flDetailedDeviceTypeEq = flDetailedDeviceTypeEq;
        return this;
    }

    public ShowStatRequest withFlConnectionTypeEq(String flConnectionTypeEq) {
        this.flConnectionTypeEq = flConnectionTypeEq;
        return this;
    }

    public ShowStatRequest withFlGenderEq(String flGenderEq) {
        this.flGenderEq = flGenderEq;
        return this;
    }

    public ShowStatRequest withFlAgeEq(String flAgeEq) {
        this.flAgeEq = flAgeEq;
        return this;
    }

    public ShowStatRequest withFlShowsGt(String flShowsGt) {
        this.flShowsGt = flShowsGt;
        return this;
    }

    public ShowStatRequest withFlShowsLt(String flShowsLt) {
        this.flShowsLt = flShowsLt;
        return this;
    }

    public ShowStatRequest withFlShowsEq(String flShowsEq) {
        this.flShowsEq = flShowsEq;
        return this;
    }

    public ShowStatRequest withFlClicksGt(String flClicksGt) {
        this.flClicksGt = flClicksGt;
        return this;
    }

    public ShowStatRequest withFlClicksLt(String flClicksLt) {
        this.flClicksLt = flClicksLt;
        return this;
    }

    public ShowStatRequest withFlClicksEq(String flClicksEq) {
        this.flClicksEq = flClicksEq;
        return this;
    }

    public ShowStatRequest withFlCtrGt(String flCtrGt) {
        this.flCtrGt = flCtrGt;
        return this;
    }

    public ShowStatRequest withFlCtrLt(String flCtrLt) {
        this.flCtrLt = flCtrLt;
        return this;
    }

    public ShowStatRequest withFlCtrEq(String flCtrEq) {
        this.flCtrEq = flCtrEq;
        return this;
    }

    public ShowStatRequest withFlSumGt(String flSumGt) {
        this.flSumGt = flSumGt;
        return this;
    }

    public ShowStatRequest withFlSumLt(String flSumLt) {
        this.flSumLt = flSumLt;
        return this;
    }

    public ShowStatRequest withFlSumEq(String flSumEq) {
        this.flSumEq = flSumEq;
        return this;
    }

    public ShowStatRequest withFlAvSumGt(String flAvSumGt) {
        this.flAvSumGt = flAvSumGt;
        return this;
    }

    public ShowStatRequest withFlAvSumLt(String flAvSumLt) {
        this.flAvSumLt = flAvSumLt;
        return this;
    }

    public ShowStatRequest withFlAvSumEq(String flAvSumEq) {
        this.flAvSumEq = flAvSumEq;
        return this;
    }

    public ShowStatRequest withFlFpShowsAvgPosGt(String flFpShowsAvgPosGt) {
        this.flFpShowsAvgPosGt = flFpShowsAvgPosGt;
        return this;
    }

    public ShowStatRequest withFlFpShowsAvgPosLt(String flFpShowsAvgPosLt) {
        this.flFpShowsAvgPosLt = flFpShowsAvgPosLt;
        return this;
    }

    public ShowStatRequest withFlFpShowsAvgPosEq(String flFpShowsAvgPosEq) {
        this.flFpShowsAvgPosEq = flFpShowsAvgPosEq;
        return this;
    }

    public ShowStatRequest withFlFpClicksAvgPosGt(String flFpClicksAvgPosGt) {
        this.flFpClicksAvgPosGt = flFpClicksAvgPosGt;
        return this;
    }

    public ShowStatRequest withFlFpClicksAvgPosLt(String flFpClicksAvgPosLt) {
        this.flFpClicksAvgPosLt = flFpClicksAvgPosLt;
        return this;
    }

    public ShowStatRequest withFlFpClicksAvgPosEq(String flFpClicksAvgPosEq) {
        this.flFpClicksAvgPosEq = flFpClicksAvgPosEq;
        return this;
    }

    public ShowStatRequest withFlAdepthGt(String flAdepthGt) {
        this.flAdepthGt = flAdepthGt;
        return this;
    }

    public ShowStatRequest withFlAdepthLt(String flAdepthLt) {
        this.flAdepthLt = flAdepthLt;
        return this;
    }

    public ShowStatRequest withFlAdepthEq(String flAdepthEq) {
        this.flAdepthEq = flAdepthEq;
        return this;
    }

    public ShowStatRequest withFlAconvGt(String flAconvGt) {
        this.flAconvGt = flAconvGt;
        return this;
    }

    public ShowStatRequest withFlAconvLt(String flAconvLt) {
        this.flAconvLt = flAconvLt;
        return this;
    }

    public ShowStatRequest withFlAconvEq(String flAconvEq) {
        this.flAconvEq = flAconvEq;
        return this;
    }

    public ShowStatRequest withFlAgoalnumGt(String flAgoalnumGt) {
        this.flAgoalnumGt = flAgoalnumGt;
        return this;
    }

    public ShowStatRequest withFlAgoalnumLt(String flAgoalnumLt) {
        this.flAgoalnumLt = flAgoalnumLt;
        return this;
    }

    public ShowStatRequest withFlAgoalnumEq(String flAgoalnumEq) {
        this.flAgoalnumEq = flAgoalnumEq;
        return this;
    }

    public ShowStatRequest withFlAgoalcostGt(String flAgoalcostGt) {
        this.flAgoalcostGt = flAgoalcostGt;
        return this;
    }

    public ShowStatRequest withFlAgoalcostLt(String flAgoalcostLt) {
        this.flAgoalcostLt = flAgoalcostLt;
        return this;
    }

    public ShowStatRequest withFlAgoalcostEq(String flAgoalcostEq) {
        this.flAgoalcostEq = flAgoalcostEq;
        return this;
    }

    public ShowStatRequest withFlAgoalroiGt(String flAgoalroiGt) {
        this.flAgoalroiGt = flAgoalroiGt;
        return this;
    }

    public ShowStatRequest withFlAgoalroiLt(String flAgoalroiLt) {
        this.flAgoalroiLt = flAgoalroiLt;
        return this;
    }

    public ShowStatRequest withFlAgoalroiEq(String flAgoalroiEq) {
        this.flAgoalroiEq = flAgoalroiEq;
        return this;
    }

    public ShowStatRequest withFlAgoalincomeGt(String flAgoalincomeGt) {
        this.flAgoalincomeGt = flAgoalincomeGt;
        return this;
    }

    public ShowStatRequest withFlAgoalincomeLt(String flAgoalincomeLt) {
        this.flAgoalincomeLt = flAgoalincomeLt;
        return this;
    }

    public ShowStatRequest withFlAgoalincomeEq(String flAgoalincomeEq) {
        this.flAgoalincomeEq = flAgoalincomeEq;
        return this;
    }

    public ShowStatRequest withStatType(StatTypeEnum statType) {
        this.statType = statType;
        return this;
    }

    public ShowStatRequest withColumns(final String columns) {
        this.columns = columns;
        return this;
    }

    public ShowStatRequest withColumnsPosition(final String columnsPosition) {
        this.columnsPosition = columnsPosition;
        return this;
    }

    public void setFileFormat(ReportFileFormat fileFormat) {
        this.fileFormat = fileFormat;
    }

    public void setStatType(StatTypeEnum statType) {
        this.statType = statType;
    }

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public String getColumnsPosition() {
        return columnsPosition;
    }

    public void setColumnsPosition(String columnsPosition) {
        this.columnsPosition = columnsPosition;
    }

    public String getCid() {
        return cid;
    }

    public ShowStatRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public String getSingleCamp() {
        return singleCamp;
    }

    public ShowStatRequest withSingleCamp(String singleCamp) {
        this.singleCamp = singleCamp;
        return this;
    }
}
