#!/usr/bin/perl

=pod

    $Id$
    
    Проверка на отсутствие в скриптах, модулях и шаблонах метки "не для продакшена" (NO_PRODUCTION)

    Этой меткой удобно пользоваться при разработке в бранче, 
    чтобы отмечать временные тестово-отладочные конструкции, 
    которые обязательно надо заменить до мержа в trunk

=cut

use warnings;
use strict;
use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;

# ищем все подходяшие файлы
# TODO: может, стоит проверять вообще все текстовые файлы?
my @exts_to_check = qw/ pm pl t tt2 html js css bemtree.xjst bemhtml conf schema.sql data.sql text /;
my $exts_re = join '|' => map {quotemeta} @exts_to_check;

my @files = (
    ( grep {-f && /\.($exts_re)$/ && ! /(no_no_production\.t|no_settings_all.t)$/} Test::ListFiles->list_repository($Settings::ROOT) ),
    ( grep {-f}  Test::ListFiles->list_repository("$Settings::ROOT/packages/yandex-direct") ),
);

Test::More::plan(tests => scalar(@files));

for my $file (@files) {
    my $text = read_file($file);
    ok($text !~ /\bNO[ _]?(?:PROD\b|PRODUCTION)/i, "NO_PRODUCTION in $file");

}
