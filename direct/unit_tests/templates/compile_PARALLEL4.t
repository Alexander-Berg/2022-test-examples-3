#!/usr/bin/perl

=pod

     $Id$
     Проверка, что все Template-шаблоны компилируются (с пустым набором переменных)

     TODO Не лучше ли вместо process использовать Template::Parser->new()->parse() ? Тогда можно не указывать FORM, FILTERS...

=cut

use warnings;
use strict;

use File::Slurp;
use Template;
use Digest::CRC qw/crc32/;
use File::Basename qw/basename/;
use Test::More;

use Test::ListFiles;
use Yandex::DBUnitTest qw/copy_table/;

use Settings;
use TTTools;

use utf8;
use open ':std' => ':utf8';

copy_table(PPCDICT, 'media_formats', with_data=>1);

my $TMPL_ROOTS = $Settings::TT_INCLUDE_PATH;

my ($par_id, $par_level) = (0, 1);
if (@ARGV == 1 && $ARGV[0] =~ /^(\d+)[:\/](\d+)$/) {
    ($par_id, $par_level) = ($1, $2);
} elsif (@ARGV) {
    die "Usage: $0 [par_id/par_level]";
}
$par_id %= $par_level;

{
no warnings 'once';
$Currencies::CURRENCY_DEBUG = 1;
}

# Что игнорировать при проверке (регвыр на имя файла)
# сразу же игнорируем все i_*, т.к. stand-alone они компилироваться вовсе не обязаны
my @SKIP_LIST = qw/ 
    ^i_ 
    ^all_regions2.tmpl.html$
    ^all_regions.tmpl.html$
/;
my $SKIP_PATTERN = "(".(join '|', @SKIP_LIST).")";

# составляем списки файлов
my (@files_to_skip, @files_to_check);
for my $file (sort grep {/\.(html|tt2)$/ && -f && crc32($_) % $par_level == $par_id} Test::ListFiles->list_repository($TMPL_ROOTS)) {
    if ( basename($file) =~ /$SKIP_PATTERN/ ){
        push @files_to_skip, $file;
    } else {
        push @files_to_check, $file;
    }
}
Test::More::plan(tests => scalar(@files_to_check));

my $template = Template->new({
    PLUGIN_BASE => 'Yandex::Template::Plugin',
    INCLUDE_PATH => $Settings::TT_INCLUDE_PATH,
    EVAL_PERL => 1,
    INTERPOLATE  => 0,
    POST_CHOMP   => 1,
    FILTERS => {
        js => sub {}, 
        typograph => sub {}, 
        idn_to_ascii => sub {},
    },
});
$template->context()->stash()->set(lang => 'ru');

# проверка на случай, если в @SKIP_LIST оказалось что-то вроде ".*"
#cmp_ok( scalar(@files_to_skip), '<=', scalar(@files_to_check) , 'suspicious: too many skipped files' );

# проверяем все по списку @files_to_check
for my $file (@files_to_check) {
    my $tt_text = read_file($file);

    my $err = compile_tt($tt_text);    
    ok(!$err, "Checking $file: $@");
}

sub compile_tt
{
    my $template_text = shift;

    # Абсолютно необходимый минимум переменных
    my $vars = {
        DOCUMENT_ROOT => "$Settings::ROOT/data",
        FORM              => {},
        banners_on_page   => 10, 
        banners => [],
        file => 'blank.html',
        head_bundles_dir => '/../unit_tests/templates/head',
    };

    eval {
        local $SIG{__WARN__} = sub {};
        my $result_text;
        $template->process(\$template_text, $vars, \$result_text) || die $template->error();
    };

    return $@;
}

