#!/usr/bin/perl -w

# $Id$

=head1 NAME

    test_alive.pl -- проверка /alive на тестовых серверах


=head1 DESCRIPTION

    Проверяет /alive на указанных портах
    в случае неудачи -- создает указанный файл 
    (полезно -- стоп-файл, тогда все заметят, что с /alive непорядок)

    Параметры
    --help 
        справка
    --host <url>
        хост, с которого брать /alive
    --port <port>, -p
        порт, который запрашивать
        можно несколько, можно через запятую: -p 80 -p 9000,14080
    --tries <num>
        сколько раз запрашивать каждый url
    --fail <num>
        порог чувствительности: ошибкой считаем <num> или более неудачных запросов к alive-локейшену
    --stop-file
        какой файл создавать/удалять в зависимости от результата проверки

    запрашиваются url'ы вида: 
    <host>:<port>/alive
    ожидается ответ "ok" со статусом 200, все другое считается ошибкой
 
    Пример: 
    test_alive.pl --host "http://localhost" -p 80 -p 9000 -p 14080 --tries 5 --fail 4 --stop-file "/var/www/ppc.yandex.ru/protected/run/web_stop.flag"

=cut

use strict;
use File::Slurp;
use Getopt::Long;
use Pid::File::Flock qw/:auto/;
use LWP::UserAgent;

use Yandex::Retry;

use utf8;
use open ':std' => ':utf8';

run() unless caller();

sub run
{
    # разбираем параметры 
    my (@ports, $HOST, $STOP_FILE, $TRIES, $FAIL_THRESHOLD);
    GetOptions(
        "help" => sub {system("podselect -section NAME -section DESCRIPTION $0 | pod2text-utf8 >&2"); exit 0;},
        "host=s" => \$HOST,
        'p|port=s'  => \@ports,
        "stop-file=s" => \$STOP_FILE,
        "tries=s" => \$TRIES,
        "fail=s" => \$FAIL_THRESHOLD,
    ) || die "can't parse options";
    @ports = split(/,/,join(',',@ports));
    die "some parameters missing" unless $HOST && $STOP_FILE && @ports && $TRIES && $FAIL_THRESHOLD;

    # запрашиваем /alive на всех требуемых портах, 
    # о неудачах пишем на STDERR -- пусть cron отсылает письма
    my $fails = 0;
    for my $p (@ports){
        my $url = "$HOST:$p/alive";
        my $problems_cnt = 0;
        my ($status, $content) = ('','');
        eval {
            retry tries => $TRIES, pauses => [1], sub {
                my $resp = LWP::UserAgent->new(timeout => 30, max_redirect => 0, ssl_opts => { verify_hostname => 0})->get($url);
                unless ($resp->is_success() && $resp->content eq 'ok'){
                    $problems_cnt++;
                    $status = $resp->code;
                    $content = $resp->content;
                    die;
                }
            };
        };
        if( $problems_cnt > 0 ){
            print STDERR "### $HOST:$p/alive problems: $problems_cnt/$TRIES\n#### Last\n".
            "status = ".$status."\n".
            "content:\n".substr($content, 0, 1000)."\n\n";
        }
        $fails++ if $problems_cnt >= $FAIL_THRESHOLD;
    }

    # работаем со стоп-файлом:
    my $stop_message = "stopped by test alive check";
    if ( $fails > 0 && ! -f $STOP_FILE ){
        # если что-то не работало, а файла еще нет -- создаем 
        print STDERR "creating stop file $STOP_FILE\n";
        write_file($STOP_FILE, {atomic => 1}, $stop_message);
    } elsif ( $fails == 0 && -f $STOP_FILE ){
        # если все ок, и есть файл, и он создан нашим же скриптом -- удаляем
        my $message = read_file($STOP_FILE, binmode => ':utf8');
        if ($message eq $stop_message){
            print STDERR "deleting stop file $STOP_FILE";
            unlink($STOP_FILE) || die "Can't unlink $STOP_FILE";
        } else {
            # если файл "чужой" -- оставляем
            print STDERR "check OK
not deleting stop file $STOP_FILE because of strange content:
'$message'
";
        }
    } 

    exit $fails; 
}

