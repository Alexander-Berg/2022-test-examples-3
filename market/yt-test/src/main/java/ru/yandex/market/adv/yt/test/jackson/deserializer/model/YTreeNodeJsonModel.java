package ru.yandex.market.adv.yt.test.jackson.deserializer.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Модель для преобразования элемента json-файла в {@link ru.yandex.inside.yt.kosher.ytree.YTreeNode}.
 * Date: 13.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@Getter
@Setter
public class YTreeNodeJsonModel {
    /**
     * Значение поля.
     */
    private String value;
    /**
     * Является ли элемент boolean.
     */
    private boolean bool;
    /**
     * Является ли элемент числом.
     */
    private boolean integer;
    /**
     * Присутствует ли у элемента знак.
     */
    private boolean signed;
}
