#!/usr/bin/perl

use warnings;
use strict;

use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;
use Yandex::Test::UTF8Builder;
use Yandex::I18n;

use utf8;
use open ':std' => ':utf8';



my $TMPL_ROOT = "$Settings::ROOT/data/t";
my $JS_ROOTS = [
    map { "$Settings::ROOT/$_" } qw(
        data/adv-blocks
        data/block
        data/js
    )
];


# js: Нормальные исключения
my @SKIP_JS = qw(
    data/block/i-translation/i-translation_lang_ua.js
    data/block/i-translation/i-translation_lang_en.js
    data/block/i-translation/i-translation_lang_tr.js
);

# js: исключения временные, до выяснения обстоятельств

my @SKIP_JS_TEMPORARY = qw(
    b-child-food-label.js
    b-child-food-labels-multiedit.js
    b-dont-show.js
    10icon-src_bem.bemjson.js
    40special-icon_bem.bemjson.js
    examples.make.js
    b-media-doubles.js
    utils.js
    forecast.js
    showSuggestion.js
    yamap_search_ballon.js
    b-baby-food-label.js
    b-campaign-select.bemtree.js
    b-campaign-stat-data.bemtree.js
    b-email-notification_extended_yes.bemtree.js
    b-showclients-list.js
    i-utils__moment.utils.js
    m-prices-constructor.js
    m-strategy-autobudget.js
    p-test-template.bemtree.js
    i-utils__moment-lang.utils.js
    i-utils__preview.utils.js
    b-badges-multiselect_type_campaigns.js
);

=head2 @INNER_TEMPLATES
=cut

my @INNER_TEMPLATES = qw(
    all_regions_en.html
    all_regions_ru.html
    all_regions.tmpl.html
    all_regions_ua.html
    all_regions_tr.html
    all_regions2_ua.html
    all_regions2_ru.html
    all_regions2_en.html
    all_regions2_tr.html
    all_regions2_en_for_ru.html
    all_regions2_ru_for_ru.html
    all_regions2_tr_for_ru.html
    all_regions2_ua_for_ru.html
    all_regions_en_for_ru.html
    all_regions_ru_for_ru.html
    all_regions_tr_for_ru.html
    all_regions_ua_for_ru.html
    media_regions_ru.html
    media_regions_ru_for_ru.html
    media_regions_ua.html
    media_regions_ua_for_ru.html
    media_regions_en.html
    media_regions_en_for_ru.html
    media_regions_tr.html
    media_regions_tr_for_ru.html
    test_cmd.html
    tmplproc_jslinks.html
    url_phrases_debug_print.html
    translations.i18n.html
);


=head2 @INNER_PATHS

    какие каталоги (считая от $Settings::ROOT/data/t) считаем внутренними, имена без регулярных выражений

=cut

my @INNER_PATHS = qw(
    admin
    catalog
    fakeadm
    static
    dev
);

=comment

 # проверка на файлы которых уже нет
 # запускать из корня беты: perl unit_tests/templates/iget.t

 my %exists_files = map {$_ => 1} map {s[^.+/][]; $_} grep {-f $_} Test::ListFiles->list_repository();

 for my $file (@SKIP_JS_TEMPORARY, @SKIP_JS, @SKIP_TEMPLATES) {
     print "$file\n" unless $exists_files{$file};
 }

 exit 0;

=cut

my $SKIP_TEMPLATES_PATTERN = "(".join('|', map { "\Q$_\E" } @INNER_TEMPLATES).")";
my $SKIP_PATHS_PATTERN = "(".join('|', map { "\Q$_\E" } @INNER_PATHS).")";
my $SKIP_JS_PATTERN = "(".join('|', map { "\Q$_\E" } @SKIP_JS, @SKIP_JS_TEMPORARY).")";

my @templates = sort grep {-f && /\.(?:html|t)$/i && ! m!^\Q$TMPL_ROOT\E/$SKIP_PATHS_PATTERN! && ! m!/$SKIP_TEMPLATES_PATTERN$! } Test::ListFiles->list_repository($TMPL_ROOT);

my @js_files = sort grep {-f && /\.(?:js|bemtree[.]js)$/i && ! m!/$SKIP_JS_PATTERN$! && !/test\.js$/ && !/sandbox\.js$/ && !/example\.js$/ } Test::ListFiles->list_repository($JS_ROOTS);
# @js_files = ();

Test::More::plan(tests => scalar(@templates + @js_files));

# Шаблоны
for my $file (@templates) {
    my $template = read_file($file, binmode => ':utf8');

    # удаляем директивы-комментарии
    $template =~ s/\[%#.*?%\]//gsm;

    # удаляем комментарии из RAWPERL
    $template =~ s/(\[%[~-]?\s*RAWPERL\s*[~-]?%\])(.*?)(\[%[~-]?\s*END\s*[~-]?%\])/$1.delete_perl_comments($2).$3/gse;
    # отделяем директивы от простого текста
    my @d = $template =~ /(\[%.*?%\])/gsm;
    $template =~ s/(\[%.*?%\])//gsm;
    my $directives = join "\n", @d;

    # из html-текста убираем комментарии и js
    $template =~ s/<!--.*?-->//gsm;
    my @inline_js = $template =~ m!(<script.*?</script>)!gsm;
    push @inline_js, ($template =~ m!onsubmit="([^"]*)"!gsm);
    push @inline_js, ($template =~ m!onsubmit='([^"]*)'!gsm);
    my $inline_js = clean_js(join "\n", @inline_js);

    $template =~ s!<script.*?</script>!!gsm;
    $template =~ s!onsubmit="([^"]*)"!!gsm;
    $template =~ s!onsubmit='([^"]*)'!!gsm;

    # из директив убираем скобки и однострочные комментарии
    $directives =~ s/(\[%|%\])//gsm;
    $directives =~ s/(?<!&)#[^\n]*//gsm; # не принимаем html-entity за однострочный комментарий

    # текст, обрабатываемый iget'ами, выкидываем 
    for my $text ($directives, $inline_js, $template) {
        $text =~ s/(?:iget|iget_noop|piget_array)\s*\(\s*\Q$_\E[^\Q$_\E]*?\Q$_\E(?:[^\)]*)?\)//gsm for qw/ ' " /;
    }

    my @rstr = "$template\n$directives\n$inline_js" =~ /([а-яА-Я][а-яА-Я ,\.\-]+)/g;

    file_ok( $file, \@rstr );
}

# js-файлы
for my $file (@js_files) {
    my $text = read_file($file, binmode => ':utf8');

    $text = clean_js($text);

    my @rstr = $text =~ /([а-яА-Я][а-яА-Я ,\.\-]+)/g;
    file_ok( $file, \@rstr );
}

# очень наивная реализация удаления комментариев из perl-кода
sub delete_perl_comments {
    my $text = shift;
    $text =~ s/^\s*#.*//gm;
    return $text;
}

sub clean_js
{
    my $text = shift;

    # исключаем строки, помеченные специальным комментарием "//iget:ignore"
    $text =~ s/^.*?\/\/\s*iget:ignore.*$//gm;

    # убираем комментарии
    $text =~ s!//[^\n]*!!gsm;

    # убираем популярные регекспы с русскими буквами
    $text =~ s/\[[\.,бБюЮ<>\\]+\]//gsm;
    $text =~ s/(а-я|А-Я)//gsm;
    $text =~ s/М-//gsm;
    $text =~ s/\bяндекс\.рф\b//gsm; # проверка на TLD рф в регулярках
    $text =~ s/\bрф\b//gsm; # проверка на TLD рф в регулярках
    $text =~ s/\Qг\\.?\\s*|город\E//; # особый регексп, подумать

    # текст, обрабатываемый iget'ами, выкидываем 
    $text =~ s/(?:iget|iget_noop|piget_array)\s*\(\s*\Q$_\E[^\Q$_\E]*?\Q$_\E(?:[^\)]*)?\)//gsm for qw/ ' " /;

    # убираем однострочные комментарии
    # убирать их вначале нельзя, т.к. внутри iget'ов часто бывают url с двумя слешами (https://)
    # убирать многострочные комментарии надо заранее, потому что внутри них могут быть неполные iget'ы, это допустимо
    $text =~ s!/\*.*?\*/!!gsm;

    return $text;
}

sub file_ok {
    my ( $file, $rstr ) = @_;

    if (@$rstr) {
        my $rtext = join "\n>>    ", @$rstr; 
        fail("$file: Russian text outside of iget:\n>>    $rtext");
    } else {
        pass($file);
    }
}
