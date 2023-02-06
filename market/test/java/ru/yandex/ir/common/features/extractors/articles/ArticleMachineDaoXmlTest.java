package ru.yandex.ir.common.features.extractors.articles;

import org.junit.jupiter.api.Test;
import ru.yandex.utils.string.aho.Vertex;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ArticleMachineDaoXmlTest {
    @Test
    public void testExtractArticle() {

        ArticleMachineDaoXml atd = new ArticleMachineDaoXml();
        Vertex<Integer> articleMachine = atd.getArticleMachine();
        checkArticles(articleMachine,
            new String[]{"Тушь объемная для ресниц MAXI VITAMIN GT860 1шт", ""},
            new String[]{"GT860"}
        );
        checkArticles(articleMachine,
            new String[]{"", "Тушь объемная для ресниц MAXI VITAMIN 1шт"},
            new String[]{}
        );
        checkArticles(articleMachine,
            new String[]{"Подвязка SENTIMENT Charmante CLZ 880805", ""},
            new String[]{"880805"}
        );
        checkArticles(articleMachine,
            new String[]{"Лампа светодиодная G13 18W 4000K трубчатая матовая", ""},
            new String[]{}
        );
    }

    private void checkArticles(Vertex<Integer> articleMachine, String[] pseudoOffer, String[] result) {
        Set<String> articles = ArticleUtils.extractArticles(articleMachine, pseudoOffer[0], pseudoOffer[1])
            .stream().map(Article::getName).collect(Collectors.toSet());
        Set<String> resultSet = Arrays.stream(result).collect(Collectors.toSet());
        assertEquals(articles, resultSet);
    }

}