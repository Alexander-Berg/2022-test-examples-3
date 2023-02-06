package ru.yandex.market.fintech.banksint.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringUtilsTest {

    @Test
    void camelToSnakeTest() {
        assertEquals("download_scoring_data_from_yt_job", StringUtils.camelToSnake("DownloadScoringDataFromYtJob"));
        assertEquals("category_tree_yt_repository", StringUtils.camelToSnake("CategoryTreeYtRepository"));
        assertEquals("cool_mds3_name", StringUtils.camelToSnake("CoolMds3Name"));
    }
}
