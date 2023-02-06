#!/usr/bin/perl

=pod

    $Id$
    Проверка консистентности существования и использования html-шаблонов

    Есть неточность: сравниваются только имена файлов, без каталогов. 
    Если обрабатывается 'admin/useful_tmpl.html', файла 'data/t/admin/useful_tmpl.html' нет, но есть 'data/t/useful_tmpl.html' -- тест это пропустит.

=cut

use warnings;
use strict;

use Encode;
use File::Slurp;
use File::Basename qw/basename/;
use List::MoreUtils qw/uniq/;
use Test::More;

use Test::ListFiles;

use Settings;

use utf8;
use open ':std' => ':utf8';

#..........................................................................................................
# Начало "настроек": регвыры, игнор-списки и т.п. С подозрительными игнор-списками надо разбираться, нормальные дополнять по необходимости.
# Подозрительное: вторая часть @FILE_OR_BLOCK_EXISTS_SKIP_LIST

# Какие файлы считаем шаблонами, регвыр на окончание имени файла
# '$' в конце не нужен -- при поиске файлов и так добавим, а при поиске обработки шаблонов в perl-коде вреден
my @TEMPLATE_FILE_REGEXP = qw/
    \w+\.html 
    \w+\.tpl 
    \w+\.tt2 
/;

# Какие файлы считаем perl-кодом (регвыр на окончание имени файла)
my @PERL_FILE_REGEXP = qw/
    \w+\.pm 
    \w+\.pl 
/;

# Что игнорируем при инспекции perl-кода: 
# если встречается обработка такого шаблона (в каком угодно perl-файле) -- делаем вид, что не заметили
# Нормальный игнор-лист: шаблоны с интерполяцией переменных + генерируемые (не хранимые в svn)
my @USED_FROM_PERL_GLOBAL_SKIP_LIST = qw/
    ^.*\$.*$ 
    ^(?:advq_|)regions_(?:ru|ua|en)\.html$
    ^archive_(?:en|rus|ukr|tr)\.html$
    ^news(?:|_en|_ukr|_tr)\.html$
    ^office_contacts\.html$
    ^(header|footer)\.html$
    ^archive\%s\.html$
    ^api_developer_offer.html$
    ^commander\.tt2$
/;
# Точечные исключения при инспеции perl-кода -- хеш: имя perl-файла => массив имен шаблонов
# Сейчас по факту не нужен (archive_* записаны в безусловный список), но на случай разных непредвиденных обстоятельств пусть будет
my %USED_FROM_PERL_SKIP = (
    'PPCLoginBox.pm' => [ 'archive_rus.html', 'archive_en.html', 'archive_ukr.html', 'archive_tr.html' ],
    'docker_build.pl' => [ 'Dockerfile-perl.tt2' ],
    'runtests.pl' => [ 'coverage.html' ],
    'collect_coverage.pl' => [ 'coverage.html' ],
    'stop_and_collect_coverage.pl' => [ 'coverage.html' ],
    'Beta.pm' => [ '.beta1.direct.yandex.ru -t ?.sandbox.html' ],
    'ModLicenses.pm' => ['moderation-detective.html', 'moderation-dietarysuppl.html', 'moderation-explosions.html', 'moderation-med-equipment.html',
        'moderation-med-services.html','moderation-pharmacy.html', 'moderation-pseudoweapon.html', 'moderation-transport.html',
        'moderation-optics.html', 'moderation-not-medicine.html', 'moderation-acids.html', 'moderation-psychology.html', 'moderation-veterinary.html',
        'moderation-popular-medicine.html', 'moderation-alcohol.html', 'moderation-tobacco.html', 'moderation-loan.html', 'moderation-insurance.html',
        'moderation-banks.html', 'moderation-maternity-capital.html', 'moderation-pawnshop.html', 'moderation-credit-consumer-cooperative.html',
        'moderation-mfi.html', 'moderation-payment.html', 'moderation-forex.html', 'moderation-forex.html', 'moderation-binary-options.html',
        'moderation-tech-inspection.html', 'moderation-sports-nutrition.html', 'moderation-gamble.html'],
);

# Что игнорировать при инспекции html-кода (регвыр на имя _подключаемого_ файла/блока)
# если встречается обработка такого шаблона -- делаем вид, что не заметили
# Нормальный игнор-лист: 
#   сразу же ингнорируем имена, записанные в переменные (непонятно, как их можно было бы проверить)
#   + игнорируем файлы, генерируемые update_informerstat.pl и getNews.pl
#   + header_local, который подключается в TRY, и в svn'е его не должно быть
#   + игнорируем файлы из svn:externals
# TODO научить svn_files получать список externals-файлов, 
# для них проверять: файлы должны существовать, но не обязаны использоваться
my @USED_FROM_HTML_SKIP_LIST = qw/ 
    ^\$ 
    ^informer_users\.txt$
    ^header_local\.html$
    ^office_contacts\.html$

    ^i-common\.tt2$
    ^b-form-input\.tt2$
	^b-form-radio\.tt2$
    ^l-head\.tt2$
    ^b-statcounter\.tt2$
    ^b-pager\.tt2$
    ^b-dropdown\.tt2$
    ^b-dropdowna\.tt2$
    ^b-form-button\.tt2$
    ^b-form-switch\.tt2$
    ^b-form-checkbox\.tt2$
    ^b-popupa\.tt2$
    ^b-domik\.tt2$
    ^b-pseudo-link\.tt2$
/;

# Что игнорировать при инспеции используемости файлов/блоков (регвыр на имя файла)
# Если обнаруживаем, что такой файл/блок не используется -- делаем вид, что не заметили
# Нормальный игнор-лист: эти файлы используются, но особым образом
my @FILE_OR_BLOCK_EXISTS_SKIP_LIST = (
    # используется экзотически: суффикс шаблона передается в параметрах формы
    '^tmplproc_jslinks.html$',
    # статические html'и
    '^popupUrlSuggestions.html$',
    '^popupSuggestions.html$',
    # имена шаблонов для печати конструируются регекспами в cmd_showCampStat (хорошо ли?)
    '^campaign_print.html$',
    '^campaign_stat_pages_print.html$',
    '^campaignstat_print.html$',
    '^campaignphrasedetal_print.html$',
    '^campaign_stat_geo_print.html$',
    # примеры использования Лего-компонент
    '^.*\.example.html$',

    # см. etc/translation.conf
    '^i-translation.tt2$',

    '^i-time-logger__init\.tt2$',
    '^i-rum-timing__init\.tt2$',

    # файлы используются в mk_regions.pl, но их имена составляются в рантайме
    '^all_regions.tmpl.html$',
    '^all_regions2.tmpl.html$',
    '^all_regions2_en.html$',
    '^all_regions2_tr.html$',
    '^all_regions2_ua.html$',
    '^all_regions2_ru.html$', # не известно, используется ли; раз другие языки в исключениях, то пусть и ru будет

    '^all_regions_en.html$',
    '^all_regions_ru.html$',
    '^all_regions_tr.html$',
    '^all_regions_ua.html$',

    '^all_regions2_en_for_ru.html$',
    '^all_regions2_ru_for_ru.html$',
    '^all_regions2_tr_for_ru.html$',
    '^all_regions2_ua_for_ru.html$',

    '^all_regions_en_for_ru.html$',
    '^all_regions_ru_for_ru.html$',
    '^all_regions_tr_for_ru.html$',
    '^all_regions_ua_for_ru.html$',

    '^media_regions_en.html$',
    '^media_regions_ru.html$',
    '^media_regions_tr.html$',
    '^media_regions_ua.html$',

    '^media_regions_en_for_ru.html$',
    '^media_regions_ru_for_ru.html$',
    '^media_regions_tr_for_ru.html$',
    '^media_regions_ua_for_ru.html$',

    'b-campaigninfo-show__optimize__ready-easy.tt2',
    'b-campaigninfo-show__optimize__accept-popup_type_optimized-camp.tt2',
    'b-campaigninfo-show__optimize__accept-popup_type_first-aid.tt2',
    'b-campaigninfo-show__optimize__reject-popup_type_optimized-camp.tt2',
    'b-campaigninfo-show__optimize__reject-popup_type_first-aid.tt2',
);
#   !!! Подозрительный игнор-лист: на этих файлах/блоках тест падает (подозрение, что они нигде не используются), почему -- надо разбираться. 
#   Если файл используется, но экзотическим образом -- переносить его в предыдущий список
push @FILE_OR_BLOCK_EXISTS_SKIP_LIST, ( 
    '^b-model_name_sms-notification.tt2$',

    '^translations\.i18n\.html$',

    # после отдельного размещения, TODO поправить позже (*.i18n не подключены, b_phrases_list__phrase__price - блок есть но правда не используется)
    'b_phrases_list__phrase__price',

    # после редизайна параметров кампании, надо разобраться
    'b_vcard_form__tbody',
    'b_campaign_form__popup__separator',

    # форма регистрации приложений API
    'api_certification_request_adminlist.html',
    'api_certification_request_blocks.html',
    'dropdown',
    'calendar',
    'select',
    'form',
    'attach',
    'filelist',
    'ctrllist',
);
# приехал при переезде externals'ов: DIRECT-94510
# непонятно, насколько нужен
push @FILE_OR_BLOCK_EXISTS_SKIP_LIST, (
#    data/lego/tools/jirabv/jirabv.html
    'jirabv.html',
);

# директории, где искать шаблоны и perl-код
# не все шаблоны из adv-blocks обязаны использоваться в Директе
my $TMPL_ROOTS = [grep {!/adv-blocks/} @$Settings::TT_INCLUDE_PATH];
my $PERL_ROOT = "$Settings::ROOT";


# Конец "настроек", дальше тест. Трогать только если очень надо. 
#..........................................................................................................

# переделываем регвыры и списки в более удобные форматы

my $TEMPLATE_FILE_PATTERN = "(?:".(join '|', @TEMPLATE_FILE_REGEXP).")";

my $PERL_FILE_PATTERN = "(".(join '|', @PERL_FILE_REGEXP).")";

my $USED_FROM_PERL_GLOBAL_SKIP_PATTERN = "(".(join '|', @USED_FROM_PERL_GLOBAL_SKIP_LIST).")"; 

# игнор-лист хотим такой: имя perl-файла => хеш { имя шаблона => 1 }
for my $p (keys %USED_FROM_PERL_SKIP){
    $USED_FROM_PERL_SKIP{$p} = { map { $_ => 1 } @{$USED_FROM_PERL_SKIP{$p}} }; 
}

my $USED_FROM_HTML_SKIP_PATTERN = "(".(join '|', @USED_FROM_HTML_SKIP_LIST).")";

my $FILE_OR_BLOCK_EXISTS_SKIP_PATTERN = "(".(join '|', @FILE_OR_BLOCK_EXISTS_SKIP_LIST).")";

#..........................................................................................................

# смотрим, какие файлы шаблонов существуют
my %FULL_PATH;
my @template_files;
my %FILE_EXISTS;
for my $file (grep {-f} Test::ListFiles->list_repository($TMPL_ROOTS)) {
    next if $file =~ m!/data/lego/blocks/!;
    my $basename = basename($file);
    if ($basename =~ /$TEMPLATE_FILE_PATTERN$/ && -f $file) {
        push @template_files, $file;
        $FILE_EXISTS{$basename} = 1;
        push @{$FULL_PATH{$basename}}, $file;
    }
}

# USED_FROM_HTML: (файл/блок => [список файлов, в которых он используется] )
my %BLOCK_EXISTS;
my %USED_FROM_HTML;
for my $file (@template_files){
    my $tt_text = Encode::decode "utf8", scalar read_file($file);

    # вместо [^\s%;,]+ хотелось бы написать просто [^\s]+ ...
    my @used_list = $tt_text =~ /(?:\s)(?:INSERT|PROCESS|INCLUDE|WRAPPER)(?:[\s\n]+)([^\s%;,~]+)/g;
    foreach (map {split /\n/, $_} ($tt_text =~ /(?:PROCESS)([^%;~]+)/sg)) {
        next unless /\+/;
        s/\s+|\+//g;
        push @used_list, $_ if $_;
    }
    
    # учитываем подключения через require_i18n:
    my @require_i18n = ($tt_text =~ /require_i18n\s*=\s*'([^']+)'/g, $tt_text =~ /require_i18n\s*=\s*"([^"]+)"/g);
    push @used_list, map {split ','} @require_i18n; 
    for ( @used_list ){
        # имя файла может быть в кавычках -- кавычки убираем
        s!^"(.*)"$!$1!;
        s!^'(.*)'$!$1!;
        # оставляем только имя файла, без каталогов
        s!^[^% ]*/!!;
    }
    push @{$USED_FROM_HTML{$_}}, $file for grep {!/$USED_FROM_HTML_SKIP_PATTERN/} @used_list;

    my @blocks = $tt_text =~ /\[%[-+=~]?\s+(?:BLOCK)\s+([^\s;]+)/g;
    for ( @blocks ){
        # имя блока может быть в кавычках -- кавычки убираем
        s!^"(.*)"$!$1!;
        s!^'(.*)'$!$1!;
    }
    $BLOCK_EXISTS{$_} = 1 for @blocks;
}

# составляем список perl-файлов
my @perl_files;
for my $file (grep {-f} Test::ListFiles->list_repository($PERL_ROOT)) {
    my $basename = basename($file);
    if ($basename =~ /$PERL_FILE_PATTERN$/ && -f $file) {
        push @perl_files, $file;
    }
}

# USED_FROM_PERL: 
# (имя шаблона => [список perl-файлов, в которых он используется] )
my %USED_FROM_PERL;
for my $filename_full (@perl_files){
    my $tt_text = read_file($filename_full);

    # выбираем все, похожее на имена шаблонов
    my @used_list =  $tt_text =~ /'([^'*]+$TEMPLATE_FILE_PATTERN)'/g;
    push @used_list, $tt_text =~ /"([^"*]+$TEMPLATE_FILE_PATTERN)"/g;
    # оставляем только имя файла, без каталогов
    map {s!^.*/([^/]*)$!$1!} @used_list;
    # уникализируем
    @used_list = uniq @used_list;

    (my $filename = $filename_full) =~ s!^.*/([^/]*)$!$1!;
    push @{$USED_FROM_PERL{$_}}, $filename_full for grep { !/$USED_FROM_PERL_GLOBAL_SKIP_PATTERN/ && !$USED_FROM_PERL_SKIP{$filename}->{$_} } @used_list;
}

# Сколько будет тестов: 
#   для каждого существующего файла проверим, что используется + 
#   для каждого используемого -- что существует
Test::More::plan(tests => scalar(keys %USED_FROM_PERL) + scalar(keys %USED_FROM_HTML) + scalar(keys %FILE_EXISTS) + scalar(keys %BLOCK_EXISTS));

#   для каждого существующего файла проверим, что используется + 
for my $used_file (keys %USED_FROM_PERL){
    ok($FILE_EXISTS{$used_file}, "$used_file seems to be nonexistent but used in ".join(", ", @{$USED_FROM_PERL{$used_file}}));
}

for my $used_file_or_block (keys %USED_FROM_HTML){
    ok($FILE_EXISTS{$used_file_or_block} || $BLOCK_EXISTS{$used_file_or_block}, 
        "$used_file_or_block seems to be nonexistent but used in ".join(", ", @{$USED_FROM_HTML{$used_file_or_block}}));
}

#   для каждого используемого -- что существует
for my $existent_file (keys %FILE_EXISTS){
    my $ok = $existent_file =~ $FILE_OR_BLOCK_EXISTS_SKIP_PATTERN ? 1 : $USED_FROM_PERL{$existent_file} || $USED_FROM_HTML{$existent_file};
    ok($ok, "file $existent_file may be unused (full path: ".join(', ', @{$FULL_PATH{$existent_file}}).")");
}

for my $existent_block (keys %BLOCK_EXISTS){
    my $ok = $existent_block =~ $FILE_OR_BLOCK_EXISTS_SKIP_PATTERN ? 1 : $USED_FROM_HTML{$existent_block};
    ok($ok, "block $existent_block may be unused");
}

