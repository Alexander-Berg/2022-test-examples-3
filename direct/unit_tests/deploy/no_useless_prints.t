#!/usr/bin/perl

=pod

    $Id$

    В скриптах миграции не должно быть отладочного вывода на консоль. 

    На консоль писать что-либо можно только в случае аварии, 
    которая требует от админа немедленных действий, 
    и тогда можно использовать die.

    Во всех остальных случаях лучше ограничиться выводом в лог. 

    В таблице bids есть поле warn. Работать с ним в миграциях надо обязательно с указаним алиаса таблицы: bi.warn, тест это пропустит.

=cut

=pod

    Тест не зависит от рабочей копии директа и может быть запущен отдельно
    Для этого ему нужно передать аргументом командной строки путь до папки с миграциями

    NB! Этот режим используется при запуске в buildbot, и если тест начнет зависеть от чего-то еще,
    то там он сломается. В таком случае стоит исключить отдельный запуск этого теста в buildbot.
    Об этом можно попросить ppalex@ или lena-san@.

=cut

use warnings;
use strict;

use Path::Tiny;
use Test::More;

# старые скрипты-исключения. TODO Когда они переедут в архив -- убрать и отсюда
my $skip_regexp = join "|", qw/
    20160114_resend-moderation-commands-without-cid.pl
/;

my $DEPLOY_ROOT;
if ($ARGV[0]) {
    die "deploy folder doesn't exists" unless -d $ARGV[0];
    $DEPLOY_ROOT = path($ARGV[0])->realpath->stringify;
} else {
    $DEPLOY_ROOT = path( path($0)->dirname  )->child("../../deploy")->realpath->stringify;
}

require "$DEPLOY_ROOT/../protected/Test/ListFiles.pm";

my @files = map { path($_) } sort grep { m{\.pl$} && !m{/deploy/archive/} && !m{/($skip_regexp)$} } Test::ListFiles->list_repository($DEPLOY_ROOT);

Test::More::plan(tests => 2*scalar(@files));

for my $file (@files) {
    my $text = $file->slurp;
    # запрещаем явные print'ы
    ok($text !~ /\bprint\b/, "'print' in deploy $file");
    # запрещаем warn "...", но пропускаем bi.warn (поле в таблице bids)
    ok($text !~ /(^|[^\.])\bwarn\b/, "'warn' or '\$log->warn' in deploy $file");
}
