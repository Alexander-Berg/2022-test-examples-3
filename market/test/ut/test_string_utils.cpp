#include <market/sitemap/util/string.h>
#include <library/cpp/testing/unittest/gtest.h>


TEST(TestUtil, Translit)
{
    //простой lowercase со стрипом
    ASSERT_EQ(Slug(" AbCdE123\t "), "abcde123");
    //не alnum символы заменяют на дефис
    ASSERT_EQ(Slug("A\t1-2^3_b"), "a-1-2-3-b");
    //несколько дефисов подряд сжимаются до 1, в начале и конце удаляются
    ASSERT_EQ(Slug("-A-B-\tC(-)D^&*%$E$"), "a-b-c-d-e");
    //все буквы кириллицы
    ASSERT_EQ(Slug("А Б В Г Д Е Ё Ж З И Й К Л М Н О П Р С Т У Ф Х Ц Ч Ш Щ Ъ Ы Ь Э Ю Я"),
                          "a-b-v-g-d-e-e-zh-z-i-i-k-l-m-n-o-p-r-s-t-u-f-kh-ts-ch-sh-shch-y-e-iu-ia");
    //пантаграмма латиницы
    ASSERT_EQ(Slug("Pack my box with five dozen liquor jugs!"),
                          "pack-my-box-with-five-dozen-liquor-jugs");
    //пантаграмма кириллицы
    ASSERT_EQ(Slug("В чащах юга жил бы цитрус? Да, но фальшивый экземпляр! Ёёё!"),
                          "v-chashchakh-iuga-zhil-by-tsitrus-da-no-falshivyi-ekzempliar-eee");
}
