package ru.yandex.travel.beans;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;

import java.util.Map;

/**
 * @author kurau (Yuri Kalinin)
 */
public class SletatOperators {

    /**
     * https://st.yandex-team.ru/VERTISTEST-196#1486392941000
     */

    @Getter
    private static Map<String, String> operators = ImmutableMap.<String, String>builder()
            .put("Ambotis",	       "Ambotis Holidays")
            .put("AnexTour",       "Anex")
            .put("Biblio Globus",  "Biblio Globus")
            .put("ICS Travel",     "ICS Travel Group")
            .put("InnaTour",       "ИННА ТУР")
            .put("Intourist",      "Интурист")
            .put("Natalie Tours",  "Natalie Tours")
            .put("Touristik",      "Touristik")
            .put("Pac Group",      "PAC GROUP")
            .put("Paks",           "Пакс")
            .put("Panteon",        "Пантеон Тревел")
            .put("PEGAS Touristic","Pegas Touristik")
            .put("Russian Express","Русский Экспресс")
            .put("Space Travel",   "Спейс Тревел")
            .put("Sunmar",         "Sunmar")
            .put("TUI",            "TUI")
            .put("Ted Travel",     "TED Travel")
            .put("TezTour",        "TezTour")
            .put("Алеан",          "Алеан")
            .put("Амиго-С",        "Amigo S")
            .put("Веди Тур Групп", "ВЕДИ ТУРГРУПП")
            .put("Музенидис",      "Mouzenidis Travel").build();
}
