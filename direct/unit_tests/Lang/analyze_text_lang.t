#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use Yandex::Test::UTF8Builder;
use Lang::Guess qw/analyze_text_lang/;
use Yandex::Queryrec qw//;

use Yandex::DBUnitTest qw/:all/;
use Settings;

use utf8;

copy_table(PPCDICT, 'ppc_properties');

# добавить тест на массив текстов
my @tests = (
    [undef, 'ウィキペディアは誰でも編集できるフリー百科事典です'],
    ['ru', 'Добро пожаловать в Википедию'],
    ['ru', 'Лишь раз за всю войну весь личный состав целого батальона за один бой был награждён орденами, а его командир стал Героем Советского Союза.'],
    ['ru', 'В этом году в первую советскую дивизию атомных подводных ракетоносцев включён головной РПКСН К-535 «Юрий Долгорукий» российского проекта 955 «Борей».'],
    ['kk', 'Уикипедияға қош келдіңіз'],
    ['kk', 'Әуеде ұшақтардың соқтығысуынан қаза болғандар саны бойынша ең зардапты апат қазақстандық әуекомпанияның қатысуымен болған.'],
    ['kk', 'Бавария футбол клубының құрылғанынан кейін, араға 90 күн салып Барселонаның негізі қаланған.'],
    ['uk', 'Ласкаво просимо до Вікіпедії'],
    ['uk', 'На одному листку найбільшої водяної рослини у світі дитина може переплисти Амазонку.'],
    ['uk', 'Папайну індустрію на Гавайях врятувала генна модифікацїя.'],
    ['tr', "Vikipedi'ye hoş geldiniz!"],
    ['tr', "Rüzgâr tulumu, rüzgârın yönünü ve şiddetini tespit etmekte kullanılan, içi hava ile dolduğunda yere paralel konuma gelen ve rüzgâr içine dönen, kumaş vb. malzemelerden üretilen bir araçtır."],
    ['tr', 'Şimdiki'],
    ['de', 'Schläfst'],
    ['be', 'Ракета стартавала, іх імкліва шпурнула ў чорную прастору.'],
);

for my $test (@tests) {
    is (analyze_text_lang($test->[1]), $test->[0], $test->[0]);
    # Функция может принимать массив текста для определения языка вцелом
    is (analyze_text_lang(split ' ', $test->[1]), $test->[0], $test->[0]);
}

# предложения содержащие пересекающиеся символы алфавита
my @tests2 = (
    ['tr', "Orjinal Redkiwi, Joyetech, Ovale, Ego Elektronik Sigaralar"],
    ['tr', "turkiyenin ilk HD teknoloji televizyonu"],
    ['tr', "Abant otelleri uygun fiyata taksitle Tatilsepeti.com'da!"],
    ['tr', "ve 12 taksit TatilSepeti'nde. En iyi fiyat garantisiyle birlikte!"],
    ['tr', "Marmaris otelleri uygun fiyata 12 taksitle Tatilsepeti.com'da!"],
    ['tr', "HDPE 100 Borular Korige Borular"],
    ['tr', "Kurumsaltek hosting profesyonel hosting hizmeti."],
    ['tr', "12-24-110-220-240 V.AC-DC Elektronik Floresan Balastlar"],
    ['tr', "Firma Sitesi 69TL, Dernek/Kurumsal Site 59TL, Genel Site 49 TL. Deneyin."],
    ['en', "deneme"],
    ['tr', "129 TL Yerine 113 TL Stoktan Teslim"],
    ['tr', "Bosch, makita, kl, dewalt, aeg. Kaliteli markalar en iyi fiyatlarla"],
    ['tr', "Izmirin Yeme Icme Rehberi ile En Guzel Restoranlara, Cafelere Hemen Ulasin"],
    ['tr', "Yandex Reklam Vermek icin Hemen Baslayin, Bilgi Hatti 444 RKLM"],
    ['tr', "istanbul Evden Eve Nakliyat, istanbul Nakliyat Bizim isimiz"],
    ['tr', "Uygun Fiyatlarlar Maldiv Ada Keyfi 7/24 Rezervasyon Burada"],
    ['tr', "Uygun fiyatlara Maldiv ada keyfi 7/24 rezervasyon burada"],
    ['en', "Samsung galaxy s3, iphone 5, galaxy note, ipad mini indirimde"],
    ['en', "Istanbul Atasehir Residence"],
    ['tr', "Istanbulda Online Arac Kiralama - Arabani Sec ve Hemen Kirala"],
    ['en', "esli komp'juter ili set' zashhishheny mezhsetevym jekranom ili proksi-serverom – ubedites', chto Firefox razreshjon vyhod v Internet."],
    ['en', "that although Steve Reich's works have often been referenced by pop and rock musicians, the Radiohead-inspired Radio Rewrite is the first time Reich (pictured) has returned the compliment"],
    ['de', "föderal"],
    ['tr', "ötesi"],
    ['en', "El idioma español"],
    # перловый queryrec очень стар и не умеет определять белорусский язык. А пересобрать нормально пока не получается :(
    #['be', 'Ракета ляцела, пакідаючы за сабой агністы хвост, пакідаючы ззаду Зямлю'],
);

{
    no warnings 'redefine';
    local *Lang::Guess::call_external_queryrec = sub {
        my $text = shift;
        return Yandex::Queryrec::queryrec($text);
    };

    for my $test(@tests2) {
        is analyze_text_lang($test->[1]), $test->[0], $test->[1];
    }
}

done_testing();
