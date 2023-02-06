#!/usr/bin/perl

=pod
    $Id: merge_camp_snapshot.t 30334 2012-04-04 07:40:17Z msa $
=cut

use strict;
use warnings;
use utf8;

use Test::More tests => 1 + 14;

# осторожно, функция any может экспортироваться и из List::MoreUtils, и из Test::Deep
use Test::Deep qw/cmp_deeply/;
use Yandex::Test::UTF8Builder;

use Storable qw/dclone/;
use List::Util qw/sum/;
use List::MoreUtils qw/any/;

BEGIN {use_ok('XLSCampImport', 'merge_camp_snapshot', 'fill_empty_price_for_phrase');};

use VCards qw/get_worktimes_array/;
use Yandex::HashUtils qw/hash_grep hash_cut/;
use Yandex::ListUtils qw/xsort/;
use PhraseText qw/ensure_phrase_have_props/;
use Currencies qw/get_currency_constant/;

my @BANNER_FIELDS = qw/body contact_info geo href phrases sitelinks title param1 param2 tags retargetings/;
my $currency = 'YND_FIXED';

{
    no warnings 'redefine';
    *Campaign::has_context_relevance_match_feature = sub($$) {0};
};

test1();
test2();
test3();
test4();
test5();
test6();
test7();
test8();
test9();
test10();
test11();
test12();
test13();
test14();

=head2 test1

    1-й тест
    если пользователь ничего не изменил в загруженом файле по сравнению с кампанией в БД,
    то никаких изменений в кампании в результате мержа не должно произойти

=cut

sub test1 {
    my $camp_from_db = get_fake_camp(groups_count => 3);
    my $camp_edited = dclone($camp_from_db);
    my $camp_should_be = dclone($camp_from_db);
    apply_fill_empty_price($camp_should_be, $camp_should_be);
    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        test_name => 'no changes in exported/imported file',
    );
}

=head2 test2

    2-й тест
    если пользователь внёс какие-то изменения в файл (удалил/создал/изменил баннер и/или фразу в нём),
    то эти изменения должны попасть в результирующую кампанию
    изменения в любом поле баннера должны приводить к изменениям в объединённой кампании

=cut

sub test2 {
    my $camp_from_db = get_fake_camp(groups_count => 1);
    my $camp_xls = dclone($camp_from_db);

    # добавляем фразы
    my $new_phrase = {phrase => 'new2 phrase'};
    ensure_phrase_have_props($new_phrase);
    push @{$camp_xls->{groups}->[-1]->{phrases}}, $new_phrase;

    my $camp_should_be = dclone($camp_xls);
    apply_fill_empty_price($camp_should_be, $camp_should_be);
    $camp_should_be->{groups}->[-1]->{_has_changes_in_phrases} = 1;
    $camp_should_be->{groups}->[-1]->{phrases}->[-1]->{_added} = 1;

    check_merge(
        camp_from_db => $camp_from_db,
        camp_edited => $camp_xls,
        camp_should_be => $camp_should_be,
        options => {},
        change_stat => {
            create_phrase => 1,
        },
        test_name => 'add new phrases',
    );
}

=head2 test3

    3-й тест
    Если внесены изменению в контактную информацию кампании, то все баннеры с такой КИ (где в графе стоит '+')
    должны быть помечены как изменённые, чтобы впоследствии изменения сохранились.

=cut

sub test3 {
    # в момент экспорта было 3 баннера: два с КИ (+) и один без неё (-)
    my $camp_from_db = get_fake_camp(groups_count => 3, banners_count => 2, phrases_count => 1);
    $camp_from_db->{groups}->[0]->{banners}->[$_]->{contact_info} = '+' for (0, 1);
    $camp_from_db->{groups}->[$_]->{banners}->[1]->{contact_info} = '-' for (1, 2);
    
    # В загруженном пользователем файле якобы произошли изменения в контактных данных (на листе Контакты),
    # также у одного из баннеров с КИ пользователь удалил плюсик из графы "Контактная информация" (что означает "не изменять КИ")
    my $camp_edited = simulate_user_edit_campaign($camp_from_db, changes => {contact_info => ['city']});
    $camp_edited->{groups}->[0]->{banners}->[1]->{contact_info} = '';

    # Ожидаемое поведение тестируемой функции:
    #     * общая КИ итоговой кампании должна совпадать с загруженной из файла
    #     * баннер с + в графе КИ должен быть помечен как _changed (т.к. изменилась КИ)
    my $camp_should_be = dclone($camp_edited);
    apply_fill_empty_price($camp_should_be, $camp_should_be);
    foreach (0..2) {
        $camp_should_be->{groups}->[$_]->{banners}->[0]->{_changed} = 1;
        $camp_should_be->{groups}->[$_]->{banners}->[0]->{save_as_draft} = 1;

    }
    $_->{_has_changes_in_banners} = 1 for @{$camp_should_be->{groups}}; 
    check_merge(
        camp_from_db => $camp_from_db,
        camp_edited => $camp_edited,
        camp_should_be => $camp_should_be,
        change_stat => {
            edit_banner => 3,
        },
        test_name => 'banner with contact information in uploaded campaign with changed contact information',
    );
}

=head2 test4

    4-й тест
    Проверяем, что при импорте меток в существующую кампанию новые метки создаются в том же регистре, что были в файле

=cut

sub test4 {
    # в момент экспорта было 3 баннера
    # в БД кампания не менялась
    my $camp_from_db = get_fake_camp(groups_count => 3);;
    # в загруженном файле добавилась новая метка
    my $camp_edited = dclone($camp_from_db);
    push @{$camp_edited->{groups}->[1]->{tags}}, {tag_name => 'НоВаЯ МеТкА'};

    # в результате после мерджа она должна сохранить свой регистр
    my $camp_should_be = dclone($camp_edited);
    apply_fill_empty_price($camp_should_be, $camp_should_be);
    $camp_should_be->{groups}->[1]->{_changed} = 1;

    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        change_stat => {edit_group => 1},
        test_name => 'регистр при создании новых меток во время импорта в существующую кампанию',
    );
}

=head2 test5

    Проверяем, что игнорируются изменения цен в автобюджетных кампаниях
    после мерджа цены не должны измениться, фразы не должны считаться изменёнными

=cut

sub test5 {
    # в БД поменялись цены (присланы автобюджетом, например)
    my $camp_from_db = get_fake_camp();
    my $camp_edited = dclone($camp_from_db);
    my $camp_should_be = dclone($camp_from_db);
    my %merge_options = (strategy => 'autobudget');
    apply_fill_empty_price($camp_should_be, $camp_should_be, %merge_options);

    # в загруженном файле менялись цены
    $_->{price} += 0.77 for @{$camp_edited->{groups}->[0]->{phrases}};

    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        change_stat => {},
        test_name => 'изменения цен в автобюджетных кампаниях',
        options => \%merge_options,
    );
}

=head2 test6

    Проверяем, что меняются ставки в сети при стратегии "независимое управление",
    отключенном поиске и ручной стратегии в сети.

=cut

sub test6 {
    # в БД поменялись цены
    my $camp_from_db = get_fake_camp(set_context_prices => 1);
    my $camp_edited = dclone($camp_from_db);
    my $camp_should_be = dclone($camp_from_db);
    my %merge_options = (strategy => 'different_places',
                         search_strategy => 'stop',
                         context_strategy => 'maximum_coverage',);
    apply_fill_empty_price($camp_should_be, $camp_should_be, %merge_options);
    $_->{price} += 0.33 for @{$camp_from_db->{groups}->[0]->{phrases}};
    $_->{price_context} += 0.34 for @{$camp_from_db->{groups}->[0]->{phrases}};
    # в загруженном файле тоже менялись цены
    $_->{price} += 0.77 for @{$camp_edited->{groups}->[0]->{phrases}};
    $_->{price_context} += 0.78 for @{$camp_edited->{groups}->[0]->{phrases}};

    # после мерджа должны получить цены на поиск из БД + цены на сеть из файла + фразы помеченные как изменённые
    $_->{price} += 0.33 for @{$camp_should_be->{groups}->[0]->{phrases}};
    $_->{price_context} += 0.78 for @{$camp_should_be->{groups}->[0]->{phrases}};
    $camp_should_be->{groups}->[0]->{_has_changes_in_phrases} = 1;
    $_->{_changed} = 1 for @{$camp_should_be->{groups}->[0]->{phrases}};

    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        change_stat => {edit_phrase => 3},
        test_name => 'изменение цен в кампании с отдельным размещением и ручной стратегии в сети',
        options => \%merge_options,
    );
}


=head2 test7

    Проверяем, что не меняются ставки при стратегии "независимое управление",
    отключенном поиске и автобюджете в сети.

=cut

sub test7 {
    # в БД поменялись цены (присланы автобюджетом, например)
    my $camp_from_db = get_fake_camp(set_context_prices => 1);
    my $camp_edited = dclone($camp_from_db);
    $_->{price} += 0.33 for @{$camp_from_db->{groups}->[0]->{phrases}};
    $_->{price_context} += 0.34 for @{$camp_from_db->{groups}->[0]->{phrases}};
    # в загруженном файле тоже менялись цены
    $_->{price} += 0.77 for @{$camp_edited->{groups}->[0]->{phrases}};
    $_->{price_context} += 0.78 for @{$camp_edited->{groups}->[0]->{phrases}};

    # после мерджа цены не должны измениться, фразы не должны считаться изменёнными
    my $camp_should_be = dclone($camp_from_db);
    my %merge_options = (strategy => 'different_places',
                         search_strategy => 'stop',
                         context_strategy => 'autobudget_week_bundle',);
    apply_fill_empty_price($camp_should_be, $camp_should_be, %merge_options);

    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        change_stat => {},
        test_name => 'изменение цен в кампании с отдельным размещением и автобюджетной стратегией в сети',
        options => \%merge_options,
    );
}

=head2 test8

    Проверяем, что изменяются ставки в сети при отдельном размещении

=cut

sub test8 {
    # в БД ничего не менялось
    my $camp_from_db = get_fake_camp(set_context_prices => 1);
    # в загруженном файле изменились цены в сети
    my $camp_edited = dclone($camp_from_db);
    $_->{price_context} += 0.77 for @{$camp_edited->{groups}->[0]->{phrases}};

    # после мерджа цены в сети должны измениться, фразы должны считаться изменёнными
    my $camp_should_be = dclone($camp_edited);
    my %merge_options = (strategy => 'different_places',
                         search_strategy => 'default',
                         context_strategy => 'maximum_coverage',);
    apply_fill_empty_price($camp_should_be, $camp_should_be, %merge_options);

    $camp_should_be->{groups}->[0]->{_has_changes_in_phrases} = 1;
    $_->{_changed} = 1 for @{$camp_should_be->{groups}->[0]->{phrases}};

    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        change_stat => {edit_phrase => 3},
        test_name => 'изменение цен в сети в кампании с отдельным размещением',
        options => \%merge_options,
    );
}

=head2 test9

    9-й тест
    если пользователь ничего не изменил в загруженом файле по сравнению с кампанией в БД,
    но поменял price_context, а стратегия не отдельное размещение
    то никаких изменений в кампании в результате мержа не должно произойти

=cut

sub test9 {
    my $camp_from_db = get_fake_camp(groups_count => 3);
    $_->{price_context} += 0.11 for @{$camp_from_db->{groups}->[0]->{phrases}};
    my $camp_edited = dclone($camp_from_db);
    $_->{price_context} += 0.21 for @{$camp_edited->{groups}->[0]->{phrases}};
    my $camp_should_be = dclone($camp_from_db);
    my %merge_options = (strategy => 'default',);
    apply_fill_empty_price($camp_should_be, $camp_should_be, %merge_options);

    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        test_name => 'no changes in exported/imported file if change price_context',
        options => \%merge_options,
    );
}

=head2 test10

    10-й тест
    Проверяем, что при изменении только сайтлинков, они должны попасть в результат

=cut

sub test10 {
    # в момент экспорта было 3 баннера
    my $camp_from_db = get_fake_camp(groups_count => 3);;

    # в загруженном файле поменялись сайтлинки
    my $camp_edited = dclone($camp_from_db);
    $camp_edited->{groups}->[0]->{banners}->[0]->{sitelinks}->[0]->{href} .= '123';
    $camp_edited->{groups}->[0]->{banners}->[0]->{sitelinks}->[0]->{title} .= 'title 123';

    # в результате после мерджа сайтлинки должны быть новые
    my $camp_should_be = dclone($camp_edited);
    apply_fill_empty_price($camp_should_be, $camp_should_be);
    $camp_should_be->{groups}->[0]->{banners}->[0]->{_changed} = 1;
    $camp_should_be->{groups}->[0]->{banners}->[0]->{save_as_draft} = 1;
    $camp_should_be->{groups}->[0]->{_has_changes_in_banners} = 1;

    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        change_stat => {edit_banner => 1},
        test_name => 'изменение только сайтлинков',
    );
}

=head2 test11

    11-й тест
    Проверяем, что при изменении только минус-слов на кампанию, никакие баннеры не меняются,
    но минус-слов в результирующей кампании изменяются

=cut

sub test11 {
    my $camp_from_db = get_fake_camp(groups_count => 3);

    # В кампании поменялись минус-слов
    my $camp_edited = dclone($camp_from_db);
    $camp_edited->{campaign_minus_words} = [qw/новые минус слова/];

    my $camp_should_be = dclone($camp_edited);
    apply_fill_empty_price($camp_should_be, $camp_should_be);

    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        change_stat => {},
        test_name => 'изменение минус-слов на кампанию',
    );
}

=head2 test12

    12-й тест
    Проверям, что при изменении минус-слов на группу, она должна пометиться как измененная

=cut

sub test12 {
    my $camp_from_db = get_fake_camp(groups_count => 3);;

    # в загруженном файле поменялись минус-слов на баннер
    my $camp_edited = dclone($camp_from_db);
    $camp_edited->{groups}->[0]->{minus_words} = [qw/новые минус слова/];
    my $camp_should_be = dclone($camp_edited);
    apply_fill_empty_price($camp_should_be, $camp_should_be);
    $camp_should_be->{groups}->[0]->{_changed} = 1;

    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        change_stat => {edit_group => 1},
        test_name => 'изменение только минус-слов на группу',
    );
}

=head2 test13

    13-й тест
    Добавили новую группу, статус группы должен быть черновик statusModerate == 'New'

=cut

sub test13 {
    # в момент экспорта было 3 баннера
    my $camp_from_db = get_fake_camp(groups_count => 3);
    # различные статусы модерации на у существующих групп (они должны сохраниться)
    $camp_from_db->{groups}->[0]->{statusModerate} = 'Yes'; 
    $camp_from_db->{groups}->[1]->{statusModerate} = 'Ready';
    $camp_from_db->{groups}->[2]->{statusModerate} = 'New';
    
    # в загруженном файле добавилась группы
    my $camp_edited = dclone($camp_from_db);
    my $new_group = {
        banners => [map {get_fake_banner(saved => 0)} (1..2)],
        phrases => get_fake_phrases(phrases_count => 5, saved => 0),
        relevance_match => [],
        retargetings => [],
        geo => 1,
        pid => 0,
        group_name => "Adgroup#77822101",
        tags => [],
        minus_words => [qw/бесплатно онлайн скачать/],
    };
    push @{$camp_edited->{groups}}, $new_group;

    # в результате после мерджа сайтлинки должны быть новые
    my $camp_should_be = dclone($camp_edited);
    apply_fill_empty_price($camp_should_be, $camp_should_be);
    $camp_should_be->{groups}->[3]->{statusModerate} = 'New';
    $camp_should_be->{groups}->[3]->{_added} = 1;
    $camp_should_be->{groups}->[3]->{_has_changes_in_phrases} = 1;
    $camp_should_be->{groups}->[3]->{_has_changes_in_banners} = 1;
    $camp_should_be->{groups}->[3]->{save_as_draft} = 1;
    
    foreach (@{$camp_should_be->{groups}->[3]->{banners}}) {
        $_->{_added} = $_->{save_as_draft} = 1;
        $_->{statusModerate} = 'New';

    }
    $_->{_added} = 1 foreach @{$camp_should_be->{groups}->[3]->{phrases}}; 
    
    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        change_stat => {create_group => 1, create_banner => 2, create_phrase => 5},
        test_name => 'добавлен новый баннер (сохранение статусов модерации)',
    );
}

=head2 test14

    14-й тест
    Проверям, что при изменении минус-слов на группу, она должна пометиться как измененная

=cut

sub test14 {
    my $camp_from_db = get_fake_camp(groups_count => 3);

    # в загруженном файле поменялись возрастные метки на баннер
    my $camp_edited = dclone($camp_from_db);
    $camp_edited->{groups}->[0]->{banners}->[0]->{flags} = {age=>16};

    my $camp_should_be = dclone($camp_edited);
    apply_fill_empty_price($camp_should_be, $camp_should_be);
    $camp_should_be->{groups}->[0]->{banners}->[0]->{_changed} = 1;
    $camp_should_be->{groups}->[0]->{banners}->[0]->{save_as_draft} = 1;
    $camp_should_be->{groups}->[0]->{_has_changes_in_banners} = 1;

    check_merge(
        camp_edited => $camp_edited,
        camp_from_db => $camp_from_db,
        camp_should_be => $camp_should_be,
        change_stat => {edit_banner => 1},
        test_name => 'изменение только возрастную метку на баннер',
    );
}

##############################################################################

=head2 check_merge

    Вспомогательная функция для вызова тестируемой функции merge_camp_snapshot и сравнения результатов с эталонными.
    Ничего сильно умного не делает, просто общий кусок кода из тестов.

    merge_camp_snapshot(
        camp_at_export => $c1,    # кампания, какой она была в момент выгрузки
        camp_from_db => $c2,      # кампания, как она сохранена в БД
        camp_edited => $c3,       # кампания из загруженного файла
        camp_should_be => $c3_should_be,    # эталонная кампания, какой мы ожидаем её видеть после мержа
        errors => ['error1', 'error2'],     # ошибки, которые мы ожидаем, что возникнут при мерже
        change_stat => {             # статистика произошедших изменений в кампании по результатам мержа
            create_banner => 123,    # созданно баннеров
            edit_banner => 123,      # изменено баннеров (за исключением фраз)
            remove_banner => 123,    # удалено баннеров
            create_phrase => 123,    # создано новых фраз (влючая фразы в новых баннерах)
            edit_phrase => 123,      # изменено фраз в существующих баннерах
            remove_phrase => 123,    # удалено фраз
        },
        options => {strategy => 'default', ignore_sitelinks_2nd_change => 1, ...}, # опции для merge_camp_snapshot; необязательный
    );

=cut

sub check_merge {
    my (%OPT) = @_;

    $OPT{options}->{currency} = $currency;

    my @result = merge_camp_snapshot($OPT{camp_from_db}, $OPT{camp_edited}, $OPT{options});
    # print STDERR Dumper({result => \@result, camp_should_be => $OPT{camp_should_be}});
    delete $OPT{camp_should_be}->{is_group_format}; 
    # сортировка нужна, чтобы не привязываться к порядку следования баннеров и фраз в массивах
    sort_camp_snapshot($OPT{camp_should_be});
    sort_camp_snapshot($result[0]) if @result;
    # merge_camp_snapshot возвращает массив из 5-ти элементов:
    #     - данные объединённой кампании
    #     - массив ошибок, обнаруженных в ходе мержа
    #     - массив предупреждений при мерже
    #     - статистика изменений (сколько создано, сколько измененно)
    #     - ссылка на массив баннеров которые нужно остановить (если их нельзя удалить)

    cmp_deeply(\@result, [$OPT{camp_should_be}, $OPT{errors} || [], $OPT{warnings} || [], $OPT{change_stat} || {}, []], $OPT{test_name});
}

=head2 simulate_user_edit_campaign

    Вспомогательная функция для внесения изменений в слепок кампании.
    Поддерживается возможность изменения не всех полей, а только в объёме, достаточном для данного теста.

    На вход первым параметром принимается ссылка на хеш с данными по модифицируемой кампании.
    Остальные параметры именованные и могут быть такими:
        * contact_info -- изменения вносятся в перечисленные поля общей контактной информации кампании
          contact_info => ['city', 'contactperson', 'name', 'street'],
        * banners -- изменения вносятся в баннеры кампании.
            Ключём является индекс баннера. Возможные значения:
            + ссылка на массив == список изменяемых полей. Изменения будут внесены во все указанные поля
              при указани sitelinks и phrases будут изменены все сайтлинки и все фразы соответственно
              banners => { 6 => ['body', 'title', 'href', 'sitelinks', 'phrases', 'geo'] }
            + ссылка на хеш == { поле1 => { подполе1 => новое значение1 }, поле2 => undef }
              возможные варианты в случае указания ссылки на хеш:
                  banners => { 6 => {phrases => {3 => 'deleted'}} }    # фраза под индексом 3 будет удалена
              при указании undef изменения будут внесены аналогично случаю "ссылка на массив" выше
                  banners => { 6 => {body => undef, title => undef, href => undef, sitelinks => undef, phrases => undef, geo => undef} }
            + скалярное значение 'deleted'. Баннер с данным индексом будет удалён из кампании.
              banners => { 6 => 'deleted' }
        * add_banners -- добавляет указанное в значении число баннеров в кампанию
          add_banners => 4

    Возвращает изменённую в соответствии с аргументами кампанию. Исходная кампания, переданная на вход остаётся неизменной.

=cut

sub simulate_user_edit_campaign {
    my ($camp_original, %OPT) = @_;

    return $camp_original unless %{$OPT{changes}};

    my $camp_edited = dclone($camp_original);
    while ( my($field, $new_value) = each %{$OPT{changes}} ) {
        if ($field eq 'contact_info') {
            for my $ci_field (@$new_value) {
                if ($ci_field eq 'city' || $ci_field eq 'contactperson' || $ci_field eq 'name' || $ci_field eq 'street') {
                    $camp_edited->{contact_info}->{$ci_field} = simulate_user_edit_string ($camp_edited->{contact_info}->{$ci_field});
                } else {
                    die "don't know how to do changes in contact info field $ci_field";
                }
            }
        } elsif ($field eq 'banners') {
            while ( my($banner_idx, $banner_changes) = each %$new_value ) {
                next unless $banner_changes;
                if ( ref($banner_changes) ) {
                    my $edited_fields;
                    my $specific_changes;
                    if ( ref($banner_changes) eq 'ARRAY' ) {
                        # в виде ссылки на массив передаётся список якобы изменённых пользователем полей
                        $edited_fields = $banner_changes;
                    } elsif( ref($banner_changes) eq 'HASH' ) {
                        # в виде ссылки на хеш передаются либо конкретные изменения в полях (field => {<изменения>})
                        # либо изменённые поля на усмотрение программы (в виде field => undef)
                        $edited_fields = [ grep { not defined $banner_changes->{$_} } keys %$banner_changes ];
                        $specific_changes = hash_grep { defined } $banner_changes;
                    } else {
                        die "strange changes in banners: $banner_changes";
                    }
                    simulate_user_edit_banner($camp_edited->{banners}->[$banner_idx],
                        edited_fields => $edited_fields,
                        version => $OPT{version},
                        specific_changes => $specific_changes,
                    );
                } elsif ( $banner_changes eq 'deleted' ) {
                    delete $camp_edited->{banners}->[$banner_idx];
                    $camp_edited->{banners} = [ grep {defined} @{$camp_edited->{banners}} ];
                } else {
                    die "don't know how to do such changes in banner: $banner_changes";
                }
            }
        } elsif( $field eq 'add_banners') {
            my $count = $new_value || 1;
            my $last_banner_number = scalar @{ $camp_edited->{banners} };
            push @{ $camp_edited->{banners} }, get_fake_banner( num => ++$last_banner_number, saved => 0 ) for 1..$count;
        } else {
            die "unknown field type: $field";
        }
    }

    return $camp_edited;
}

=head2 simulate_user_edit_string

    Принимает строку, подлежающую измененияю, и (опционально) версию этого изменения.
    $edited_string = simulate_user_edit_string('test stringedited4', 6);
    $edited_string = 'test stringedited6';

=cut

sub simulate_user_edit_string {
    my ($string, $new_version) = @_;

    $string ||= '';
    $new_version ||= 1;
    # оставляем только финальную версию, т.к. нас не интересует в какой последовательности были редактирования
    $string =~ s#edited\d+$##;
    return "${string}edited$new_version";
}

=head2 simulate_user_edit_banner

    Вспомогательная функция для внесения изменений в баннер.
    Первым параметрам передаётся ссылка на хеш, описывающая изменяемый баннер.
    Остальные параметры именованные и могут быть такими:
        + edited_fields -- список изменяемых полей. Изменения будут внесены во все указанные поля
            при указани sitelinks и phrases будут изменены все сайтлинки и все фразы соответственно
            edited_fields => ['body', 'title', 'href', 'sitelinks', 'phrases', 'geo']
        + specific_changes -- ссылка на хеш с описанием изменений в конкретных подполях
          возможные варианты:
          specific_changes => {phrases => {3 => 'deleted'}} }    # фраза под индексом 3 будет удалена
    Изменения вносятся в переданный на вход баннер.

=cut

sub simulate_user_edit_banner {
    my ($banner, %OPT) = @_;

    if ( $OPT{edited_fields} ) {
        my $version = $OPT{version} || 1;
        for my $field ( @{ $OPT{edited_fields} } ) {
            if( $field eq 'contact_info' ) {
                $banner->{contact_info} = ( $version % 2 ) ? '+' : '-';
            } elsif( any {$field eq $_} qw/body title href param1 param2/ ) {
                $banner->{$field} = simulate_user_edit_string ($banner->{$field}, $version);
            } elsif( $field eq 'sitelinks' ) {
                for my $sitelink (@{$banner->{sitelinks}}) {
                    $banner->{href} = simulate_user_edit_string ($banner->{href}, $version);
                    $banner->{title} = simulate_user_edit_string ($banner->{title}, $version);
                }
            } elsif ( $field eq 'phrases') {
                for my $phrase (@{$banner->{phrases}}) {
                    $phrase->{phr} = $phrase->{phrase} = simulate_user_edit_string ($phrase->{phr}, $version);
                    $phrase->{price} = 0.05 + 0.1*$version;
                }
            } elsif ( $field eq 'geo') {
                $banner->{geo} = 1 + $version;
            } elsif ( $field eq 'tags' ) {
                if ($banner->{tags} && ref($banner->{tags}) eq 'ARRAY') {
                    for my $tag(@{$banner->{tags}}) {
                        $tag->{tag_name} = simulate_user_edit_string($tag->{tag_name}, $version);
                    }
                }
            } else {
                die "unknown banner field type: $field";
            }
        }
    }

    if ($OPT{specific_changes}) {
        while ( my($field_name, $field_changes) = each %{$OPT{specific_changes}} ) {
            if( $field_name eq 'phrases' ) {
                while ( my($phrase_idx, $phrase_changes) = each %$field_changes ) {
                    die "there's no phrase with index $phrase_idx" unless exists $banner->{phrases}->[$phrase_idx];
                    if ( $phrase_changes eq 'deleted' ) {
                        delete $banner->{phrases}->[$phrase_idx];
                        $banner->{phrases} = [ grep {defined} @{$banner->{phrases}} ];
                    } else {
                        die "don't know how to do action '$phrase_changes' with banner phrases";
                    }
                }
            } else {
                die "don't know how to apply specific changes for $field_name";
            }
        }
    }
}

=item sort_camp_snapshot

    Сортирует баннеры и фразы внутри кампании. Нужно это чтобы не был важен порядок их следования в массивах.
    Внимание, сортировка делается in-place, т.е. модифицирует переданные на вход данные.
    Также удаляет айдишники тегов, т.к. они могли поменяться и мерджим мы по тексту меток.

    $camp = {
        banners => [
            {
                phrases => [
                    {id => 123, phr => 'phrase1 text'},
                    {id => 321, phr => 'phrase2 text'},
                ],
            },
        ],
    };
    sort_camp_snapshot($camp);

=cut

sub sort_camp_snapshot($) {
    my ($camp) = @_;

    if ($camp && ref($camp) eq 'HASH' && exists $camp->{groups} && @{$camp->{groups}}) {
        for my $group (@{$camp->{groups}}) {
        
            $group->{banners} = [ xsort { ( $_->{title}, $_->{body}, $_->{href} ) } @{$group->{banners}} ];
            if (exists $group->{phrases} && @{$group->{phrases}}) {
                $group->{phrases} = [ xsort { ( $_->{id} || 0, $_->{phr} ) } @{$group->{phrases}} ];
            }
            if ($group->{tags} && ref($group->{tags}) eq 'ARRAY' && @{$group->{tags}}) {
                for my $tag (@{$group->{tags}}) {
                    delete $tag->{tag_id};
                }
            }
            $group->{minus_words} = [sort @{$group->{minus_words}}];
        }
    }
    $camp->{campaign_minus_words} = [sort @{$camp->{campaign_minus_words}}];

    return $camp;
}

=head2 get_fake_camp

    Вспомогательная функция, возвращающая ссылку на хеш, по структуре данных совпадающий со слепком кампании.
    Принимает необязательные именованные параметры:
        groups_count -- число баннеров. Если не указан, кампания будет с одним баннером.
        set_context_prices -- установить отдельные цены для сети

=cut

sub get_fake_camp {
    my (%OPT) = @_;

    my $set_context_prices = defined $OPT{set_context_prices} ? $OPT{set_context_prices} : 0;

    my $camp = {
        groups => [],
        contact_info => get_fake_contactinfo(),
        is_group_format => 1,
        campaign_minus_words => [qw/минус слова/],
        opts => '',
    };

    my $groups_count = $OPT{groups_count} || 1;
    my $banners_count = $OPT{banners_count} || 2;
    my $phrases_count = defined $OPT{phrases_count} ? $OPT{phrases_count} : 3;

    for my $num ( 1..$groups_count ) {
        push @{$camp->{groups}}, {
            adgroup_type => 'base',
            banners => [map {get_fake_banner(num => $num * 88 + $_, saved => 1)} (1..$banners_count)],
            phrases => get_fake_phrases(group_number => $num, saved => 1, phrases_count => $phrases_count, set_context_prices => $set_context_prices),
            relevance_match => [],
            retargetings => [],
            geo => 1,
            statusModerate => 'New',
            group_name => $banners_count > 1 ? "group name $num" : undef,
            pid => 98869 + $num * 6,
            tags => get_fake_tags(group_number => $num, tags_count => 3),
            minus_words => [],
        }
    }

    return $camp;
}

=head2 get_fake_banner

    Вспомогательная функция, возвращающая ссылку на хеш, по структуре данных совпадающий с баннером.
    Принимает необязательные именованные параметры:
        num -- номер баннера в кампании. используется для генерации разные данных в разных баннерах
        phrases_count -- число фраз в создаваемом баннере
        saved -- признак того, сохранён ли баннер в БД (определяет, будут ли у баннера bid)
        is_sent_to_BS -- признак того, что баннер отправлен в крутилку (определяет, будет ли у баннера BannerID)
        set_context_prices -- установить отдельные цены для сети

=cut

sub get_fake_banner {
    my (%OPT) = @_;

    my $num = $OPT{num} || 1;
    
    my $is_saved = defined $OPT{saved} ? $OPT{saved} : 1;
    my $is_sent_to_BS = defined $OPT{sent_to_BS} ? $OPT{sent_to_BS} : 0;

    return {
        ad_type => 'text',
        BannerID => $is_sent_to_BS ? 666 + $num : 0,
        banner_type => 'desktop',
        bid => $is_saved ? 123456 + $num : 0,
        body => "test bannner$num body",
        contact_info => '+',
        flags => {},
        href => "www.yandex.ru/?test$num",
        short_status => '',
        sitelinks => get_fake_sitelinks(banner_number => $num, sitelinks_count => 3),
        title => "test banner$num title",
        video_resources => {},
    };
}

=head2 get_fake_phrases

    Вспомогательная функция, возвращающая ссылку на массив с данными о сгенерированных фразах.
    Принимает необязательные именованные параметры:
        num -- номер баннера в кампании, в котором будут использоваться фразы. используется для генерации разные данных в разных баннерах.
        phrases_count -- число фраз, которое необходимо создать
        saved -- признак того, сохранён ли баннер в БД (определяет, будут ли у фраз id)
        set_context_prices -- установить отдельные цены для сети

=cut

sub get_fake_phrases {
    my (%OPT) = @_;

    my $phrases_count = defined $OPT{phrases_count} ? $OPT{phrases_count} : 3;
    my $num = defined $OPT{group_number} ? $OPT{group_number} : 1;
    my $is_saved = defined $OPT{saved} ? $OPT{saved} : 1;

    my @phrases = ();
    for my $phrnum (1..$phrases_count) {
        my $phrase = {
            id => $is_saved ? 234+$num*100+$phrnum : 0,
            phr => "test banner$num phrase$phrnum",
            phrase => "test banner$num phrase$phrnum",
            price => 0.1*$num,
            param1 => ($num % 2) ? 'test param1' : '',
            param2 => ($num % 2) ? 'test param2' : '',
            autobudgetPriority => 3,
        };
        $phrase->{price_context} = 0.15*$num if $OPT{set_context_prices};
        ensure_phrase_have_props($phrase);
        push @phrases, $phrase;
    }

    return \@phrases;
}

=head2 get_fake_contactinfo

    Вспомогательная функция, возвращающая ссылку на хеш со сгенерированной контактной информацией.

=cut

sub get_fake_contactinfo {
    my $contact_info =  {
        apart => undef,
        auto_bounds => '37.564590,55.506036,37.572800,55.510695',
        auto_point => '37.568695,55.508366',
        auto_precision => 'exact',
        build => undef,
        city => 'Москва',
        city_code => '916',
        contact_email => 'direct_noreply@yandex-team.ru',
        contactperson => 'test person',
        country => 'Россия',
        country_code => '+7',
        ext => 123,
        extra_message => '',
        house => 6,
        im_client => 'icq',
        im_login => '123456',
        manual_bounds => '37.564590,55.506036,37.572800,55.510695',
        manual_point => '37.568695,55.508366',
        name => 'Тестов Тест Тестович',
        org_details_id => undef,
        phone => '1234567',
        street => 'Ленина',
        vcard_id => 654321,
        worktime => '2#4#9#45#20#39',
    };
    $contact_info->{worktimes} = get_worktimes_array($contact_info->{worktime});

    return $contact_info;
}

=head2 get_fake_sitelinks

    Вспомогательная функция, возвращающая ссылку на массив с данными о дополнительных ссылках.
    Принимает необязательные именованные параметры:
        banner_number -- номер баннера в кампании, в котором будут использоваться доп. ссылки. используется для генерации разные данных в разных баннерах.
        sitelinks_count -- число ссылок, которое необходимо создать

=cut

sub get_fake_sitelinks {
    my (%OPT) = @_;

    my @sitelinks = ();
    my $sitelinks_count = defined $OPT{sitelinks_count} ? $OPT{sitelinks_count} : 3;
    my $num = defined $OPT{banner_number} ? $OPT{banner_number} : 1;

    for my $slnum (1..$sitelinks_count) {
        push @sitelinks, {
            href => "www.yandex.ru/?test$num&sitelink=$slnum",
            title => "test banner$num sitelink$slnum",
        };
    }

    return \@sitelinks;
}

=head2 get_fake_tags

    Вспомогательная функция, возвращающая ссылку на массив с данными о метках.
    Принимает необязательные именованные параметры:
        banner_number -- номер баннера в кампании, в котором будут использоваться метки. используется для генерации разные данных в разных баннерах.
        tags_count -- число меток, которое необходимо создать

=cut

sub get_fake_tags {
    my (%OPT) = @_;

    my @tags = ();
    my $tags_count = defined $OPT{tags_count} ? $OPT{tags_count} : 3;
    my $num = defined $OPT{group_number} ? $OPT{group_number} : 1;

    for my $tnum (1..$tags_count) {
        push @tags, {
            tag_id => 100+$tnum,
            tag_name => "test banner$num tag$tnum",
        };
    }

    return \@tags;
}

=head2 apply_fill_empty_price

    Вспомогательная функция, применяющая метод XLSCampImport::fill_empty_price_for_phrase ко всем фразам снимка кампании
    (для подготовки эталонного снимка)
    
=cut

sub apply_fill_empty_price {
    my ($camp_dest, $camp_src, %OPT) = @_;

    my $default_price = get_currency_constant($currency, 'DEFAULT_PRICE');

    my $old_groups_hash = {};
    for my $group (@{ dclone($camp_dest->{groups}) }) {
        $group->{phrases_hash} = {map {$_->{id} => $_} grep {$_->{id}} @{$group->{phrases}}};
        $old_groups_hash->{$group->{pid}} = $group;
    }

    my $new_banners = [];
    my $new_banners_bids = {};

    my $fill_price_options = hash_cut \%OPT, qw/strategy search_strategy context_strategy/;
    $fill_price_options->{default_price} = $default_price;

    for my $group (@{ $camp_src->{groups} }) {
        for my $new_phrase (@{ $group->{phrases} }) {

            if (
                   $old_groups_hash->{ $group->{pid} }
                && $new_phrase->{id}
                && $old_groups_hash->{ $group->{pid} }
                && $old_groups_hash->{ $group->{pid} }->{phrases_hash}
                && $old_groups_hash->{ $group->{pid} }->{phrases_hash}->{ $new_phrase->{id} }
            ) {
                my $old_phrase = $old_groups_hash->{ $group->{pid} }->{phrases_hash}->{ $new_phrase->{id} };

                fill_empty_price_for_phrase($new_phrase, %$fill_price_options, old_phrase => $old_phrase);
            } else {
                fill_empty_price_for_phrase($new_phrase, %$fill_price_options);
            }
        }
    }
}

