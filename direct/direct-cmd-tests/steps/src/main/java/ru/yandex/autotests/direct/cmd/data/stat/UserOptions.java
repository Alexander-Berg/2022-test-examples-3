package ru.yandex.autotests.direct.cmd.data.stat;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.stat.filter.StatMasterFilter;

import java.util.ArrayList;

public class UserOptions {

    @SerializedName("show_extended_client_info")
    private String showExtendedClientInfo;

    @SerializedName("stat_periods")
    private String statPeriods;

    @SerializedName("stat_master_filters")
    private ArrayList<StatMasterFilter> statMasterFilters = new ArrayList<StatMasterFilter>();

    @SerializedName("statistic_with_nds")
    private String statisticWithNds;

    @SerializedName("stat_master_reports")
    private ArrayList<Object> statMasterReports = new ArrayList<Object>();

       public String getShowExtendedClientInfo() {
        return showExtendedClientInfo;
    }

       public void setShowExtendedClientInfo(String showExtendedClientInfo) {
        this.showExtendedClientInfo = showExtendedClientInfo;
    }

       public String getStatPeriods() {
        return statPeriods;
    }

       public void setStatPeriods(String statPeriods) {
        this.statPeriods = statPeriods;
    }

       public ArrayList<StatMasterFilter> getStatMasterFilters() {
        return statMasterFilters;
    }

       public void setStatMasterFilters(ArrayList<StatMasterFilter> statMasterFilters) {
        this.statMasterFilters = statMasterFilters;
    }

       public String getStatisticWithNds() {
        return statisticWithNds;
    }

       public void setStatisticWithNds(String statisticWithNds) {
        this.statisticWithNds = statisticWithNds;
    }

       public ArrayList<Object> getStatMasterReports() {
        return statMasterReports;
    }

       public void setStatMasterReports(ArrayList<Object> statMasterReports) {
        this.statMasterReports = statMasterReports;
    }

}
