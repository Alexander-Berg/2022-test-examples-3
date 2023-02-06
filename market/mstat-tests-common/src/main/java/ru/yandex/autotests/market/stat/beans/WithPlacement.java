package ru.yandex.autotests.market.stat.beans;

/**
 * Created by entarrion on 28.12.15.
 */
public interface WithPlacement {
    String getPofRaw();

    void setPofRaw(String pofRaw);

    Integer getPof();

    void setPof(Integer pof);

    Integer getVid();

    void setVid(Integer vid);

    Integer getClid();

    void setClid(Integer clid);

    Integer getDistrType();

    void setDistrType(Integer distrType);

    Integer getPp();

    void setPp(Integer pp);
}
