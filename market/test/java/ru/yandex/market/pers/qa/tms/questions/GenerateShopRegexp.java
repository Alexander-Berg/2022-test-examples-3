package ru.yandex.market.pers.qa.tms.questions;

/**
 * @author varvara
 * 02.11.2018
 */
public class GenerateShopRegexp {

    private static final String[] WORDS_1 = new String[]{"достав", "гарант", "артикул", "стои", "налич", "почт", //6
        "платеж", "самовывоз", "упаковк", "прода", "рассрочк", "кредит", "приобре", "заказ", "магазин", "сертифи", //10
        "связат", "скидк", "произв", "адрес", "срок", "оригинал", "подделк"}; //7

    private static final String[] WORDS_2 = new String[]{"забрать"}; //1

    private static final String[] WORDS_3 = new String[]{"оплат", "цен", "куп", "слат", "сыл"}; //5

    public static void main(String[] args) {
        int i = 1;
        for (String s : WORDS_1) {
            System.out.println(String.format("%s,\".*?[^а-яА-Я]%s.*\"", i, s));
            i++;
            System.out.println(String.format("%s,\"^%s.*\"", i, s));
            i++;
        }

        for (String s : WORDS_2) {
            System.out.println(String.format("%s,\".*?[^а-яА-Я]%s[^а-яА-Я].*\"", i, s));
            i++;
            System.out.println(String.format("%s,\"^%s[^а-яА-Я].*\"", i, s));
            i++;
            System.out.println(String.format("%s,\"^.*[^а-яА-Я]%s\"", i, s));
            i++;
        }

        for (String s : WORDS_3) {
            System.out.println(String.format("%s,\".*?%s.*?\"", i, s));
            i++;
        }
    }
}
