#!/usr/bin/perl

# Скрипт для автоматического запуска стрельб в лунапарке, с описанием работы можно ознакомиться здесь: https://wiki.yandex-team.ru/users/vananos/nagruzochnoe-testirovnie/#firestarter

use strict;
use warnings;

use Encode qw/decode_utf8/;
use HTTP::Request::Common;
use LWP::UserAgent;
use YAML qw/LoadFile/;
use JSON qw/from_json to_json/;
use Getopt::Long;
use DateTime;
use Date::Parse qw /str2time/;

use utf8;
use open qw/:std :encoding(UTF-8)/;

my $API_BASE_URI = "https://lunapark.yandex-team.ru/api";
my $TIMEOUT = 15;
my $RETRY_COUNT = 10;

sub load_and_validate_config_file {
    my $config = LoadFile($_[0]);
    die "Не указан логин в файле конфигурации" unless($config->{login});
    die "Не найдена секция tasks в файле конфигурации" unless($config->{tasks});
    my $i = 1;
    foreach my $o (values $config->{tasks}) {
        die "Не указан обязательный параметр id в задаче номер: $i" unless($o->{id});
        die "Не указан обязательный параметр ammo в задаче номер: $i" unless($o->{ammo});
        $i++;
    }
    return $config;
}

sub upload_ammo {
    my ($file_path, $login, $id, $ua) = @_;
    my $response = $ua->request(POST "$API_BASE_URI/addammo.json", 
        Content_Type => 'form-data', 
        Content => [
            login => $login, 
            dsc => $id."_".DateTime->now->strftime('%Y%m%d'), 
            file => [$file_path]
        ]);  

    if($response->is_success) {
        my $result = from_json($response->decoded_content)->[0];
        return $result->{url} if($result->{success} eq "true"); 
    }
    die "Ошибка при загрузке патрона: $response->status_line";
}

sub fire {
    my ($task, $login, $ammo_url, $ua) =  @_;
    my $time = 1;
    my $success = 0;
    while($time <= $RETRY_COUNT) {
        printf "попытка запуска стрельбы #%d\n", $time++;

        my $response = $ua->request(POST "$API_BASE_URI/job/$task->{id}/repeat.json",
            Content => to_json({
                    "phantom.ammofile" => $ammo_url,
                    "meta.operator" => $login
                }));

        my $result;
        if($response->is_success) {
            $result = from_json($response->decoded_content)->[0];
            if($result->{success} eq "true") {
                $success = 1;
                last;
            }
        }
        printf "при запуске стрельбы произошла ошибка: %s\nожидаем %d секунд до следующей попытки\n", $result->{error}, $TIMEOUT;
        sleep $TIMEOUT;
    }
    die "Не удалось запустить стрельбу в течении $RETRY_COUNT раз" unless($success);
    sleep 30; #Ждем 30 секунд, попытка закостылить случай, когда у пользователя уже есть запущенные стрельбы, что бы мы не начали отслеживать их 
    $time = 1;
    while($time <= $RETRY_COUNT) {
        printf "попытка получения номера запущенной стрельбы #%d\n", $time++;
        my $response = $ua->request(GET "$API_BASE_URI/v2/jobs?person=$login&td__isnull=true");
        my $result;
        if($response->is_success) {
            $result = from_json($response->decoded_content);
            if($result->{objects} && @{$result->{objects}} >= 1) {
                return (sort {str2time($b->{fd}) <=> str2time($a->{fd})} @{$result->{objects}})[0]->{n}; #получаем id самой последней онлайн стрельбы    
            }
        }
        printf "попытка получения номера запущенной стрельбы завершилась неудачно: %s\n, ожидаем 5 секунд до следующей попытки\n", $response->status_line;
        sleep 5;  
    }
    die "Не удалось получить id запущенной стрельбы в течении отведенного времени\n";
}

sub wait_until_fire_is_over {
    my ($id, $ua) = @_;
    my $fails = 0;
    my $MAX_FAIL_TIMES = 10;
    while($fails < $MAX_FAIL_TIMES) {
        my $response = $ua->request(GET "$API_BASE_URI/v2/jobs/$id");
        my $result;
        if($response->is_success) {
            return if(from_json($response->decoded_content)->{td});     
        } else {
            $fails++;
            printf "попытка получения онлайн стрельб завершилась неудачно: %s\n, ожидаем 30 секунд до следующей попытки\n", $response->status_line;
        }
        sleep 30;  
    }
    die "Не удалось получить информацию о состоянии стрельбы id: $id в течеении $MAX_FAIL_TIMES раз\n";
}


sub process_task {
    my ($task, $login, $ua) = @_;
    my $ammo_file = "/tmp/ammo.tmp";
    print "генерируем патроны...\n";
    die "Ошибка во время генерации патрона" if(system "$task->{ammo} > $ammo_file");

    print "генерация патрона завершена, загружаем патроны в лунапарк...\n";

    my $ammo_url = upload_ammo $ammo_file, $login, $task->{id}, $ua;
    print "партроны успешно загружены, запускаем стрельбу...\n";
    my $id = fire $task, $login, $ammo_url, $ua;
    print "стрельба успешно запущена, id стрельбы: $id\nожидаем окончания стрельбы id: $id...\n";
    wait_until_fire_is_over $id, $ua;
    return $id;
}

my %O;

GetOptions(
    'config=s' => \$O{config},
) or die "can't parse options, stop\n";

die "укажите файл конфигурации с помощью параметра --config" unless($O{config}); 

my $config = load_and_validate_config_file $O{config};
my $i = 1;
my $ua = LWP::UserAgent->new;

foreach my $task (values $config->{tasks}) {
    printf "Обрабатываем задачу #%d с id:%d\n", $i++, $task->{id};
    my $fire_id = process_task $task, $config->{login}, $ua;
    printf "Задача с id:%d завершена, результаты доступны по ссылке: https://lunapark.yandex-team.ru/%d", $task->{id}, $fire_id;  
}
