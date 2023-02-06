#!/usr/bin/perl

# $Id$

=pod

     Миграционные записи должны быть в формате, который понимает Мигратор (migrator)

     Наверное, правильнее было бы сделать не юнит-тестом, 
     а проверкой на diff для заданной ревизии. 
     И подтягивать результаты в django-релизер. 

     TODO Подумать и переместить проверку в более правильное место

=cut

=pod

    Тест не зависит от рабочей копии директа и может быть запущен отдельно
    Для этого ему нужно передать аргументом командной строки путь до папки с миграциями

    NB! Этот режим используется при запуске в buildbot, и если тест начнет зависеть от чего-то еще,
    то там он сломается. В таком случае стоит исключить отдельный запуск этого теста в buildbot.
    Об этом можно попросить ppalex@ или lena-san@.

=cut

use Direct::Modern;

use MigratorB::Parse;
use Path::Tiny;
use Test::Exception;
use Test::More;

use List::MoreUtils qw/all/;

use open ':std' => ':utf8';

=head2 approvers

    Ревью миграций: https://wiki.yandex-team.ru/direkt/codestyleguide/migrations/
    Рекомендации по структуре данных в БД: http://wiki.yandex-team.ru/Direkt/CodeStyleGuide/DB
    Рекомендуется читать и дополнять. 


    Кроме того, миграции в Директе должны быть неизменно удобны в применении на разработческих, тестовых и продакшен конфигурациях,
    это экономит время и усилия разработчиков, релиз-менеджера, администраторов. 

    Поэтому для поддержания качества миграций: 

    * если сомневаешься в миграции (формат/содержание) -- спрашивай совета у коллег, не аппрувь "на авось" 

    * проверяй, как выглядит "скомпилированная" миграция (обязательно с ключом -s):
    migrator -t -s <path/to/migration/file>

    * если ты проаппрувил миграцию, а она оказалась проблемной (синтаксис/размер/отладочные печати/...) -- 
    напиши юнит-тест/проверку, чтобы подобная проблема больше не могла повториться

=cut

# увеличиваем немного лимит для миграции: deploy/20190514_create_template_place_and_template_resource_tables.migr.yaml
$MigratorB::Parse::MAX_ALLOWED_SQL_LENGTH += 100;

# где лежат миграционные записи
my $DEPLOY_ROOT;
if ($ARGV[0]) {
    die "deploy folder doesn't exists" unless -d $ARGV[0];
    $DEPLOY_ROOT = path($ARGV[0])->realpath->stringify;
} else {
    $DEPLOY_ROOT = path( path($0)->dirname  )->child("../../deploy")->realpath->stringify;
}

require "$DEPLOY_ROOT/../protected/Test/ListFiles.pm";

my @KNOWN_OLDSTYLE_DEPLOY_FILES = grep {$_} map { s/\s*//gr } <DATA>;
my $known_oldstyle_deploy_files_regexp = "^(?:".join("|", map {quotemeta $_} @KNOWN_OLDSTYLE_DEPLOY_FILES).")\$";

# составляем список: все новые файлы, за исключением исключений
my @files_to_check;
for my $file (map {path($_)} Test::ListFiles->list_repository($DEPLOY_ROOT)) {
    next if ! $file->is_file
        || $file =~ /\.sh$/
        || $file =~ /\.data$/
        || $file->basename =~ /$known_oldstyle_deploy_files_regexp/
        || $file =~ m!archive/!;

    push @files_to_check, $file;
}
@files_to_check = sort @files_to_check;

Test::More::plan(tests => 2 * scalar(@files_to_check));

# проверяем все по списку 
my $compiled_text;
for my $file (@files_to_check) {
    my $deploy = $file->slurp_utf8;
    lives_ok { $compiled_text = to_text($file => $deploy, die_on_errors => 1, no_plaintext => 1); } "migration syntax error in $file";
    my @approvers;
    if($compiled_text =~ /^approved\s+by\s+([a-z0-9\-, ]+)$/sm){
        @approvers = split /\s*,\s*/, $1;
    }
    # ожидаем найти правильный список аппруверов: строки (логины) через запятую.
    # Как контролировать, что строки -- логины -- непонятно 
    my $approvers_look_good = scalar @approvers > 0 && all {$_ =~ /^\S+$/} @approvers;
    ok($approvers_look_good, "Deploy $file should be appropriately approved");
}

# список исключений, пополняться в целом не должен
# допустимые пополнения -- если формат меняется несовместимым образом, и старые миграции перестают ему соответствовать
__DATA__
20150625_add_web_mobile_app.migr

