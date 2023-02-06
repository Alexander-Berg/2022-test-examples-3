package Test::Intapi;

# $Id$

=head1 NAME
    

=head1 DESCRIPTION

    Фреймворк для тестирования Директовых Intapi

    Для запуска тестов следует использовать скрипт intapi_tests/run_intapi_tests.pl

    Переэкспортирует все из полезных модулей (Test::More, Test::Exception)
    + "запускатель тестов" (если тесты описаны в нужном формате)
    + "базовый url", от которого можно отсчитывать все адреса для запросов

    Формат данных 
    см. в описании run_tests

    При выставленной переменной окружения TEST_INTAPI_VERBOSE
    печатает на STDERR данные о всех http-запросах, которые делаются через run_tests
    (чтобы можно было воспроизвести отдельный запрос wget'ом или еще как-то)

    TEST_INTAPI_BASE_URL='//beta.direct.yandex.ru:8804' prove -Iprotected intapi_tests/DirectConfiguration.t
    TEST_INTAPI_VERBOSE=1 TEST_INTAPI_BASE_URL='//beta.direct.yandex.ru:8804' prove -Iprotected intapi_tests/DirectConfiguration.t 2> STDERR

    TODO
    * (надо) расширить и на soap-вызовы тоже
    * (может быть) для response code проверки вроде "не 200", "404 или 500"

=cut

use Direct::Modern;

use Test::More;
use Test::Exception;
use Test::Deep;

use Data::Dumper;
use List::Util qw/sum/;
use Encode;
use SOAP::Lite;
use XMLRPC::Lite;
use Yandex::XMLRPC::UTF8Serializer;

use Yandex::HTTP;

use base qw/Exporter/;

# Экспортируем 
# 1. свои функции
# 2. все из Test::More, Test::Exception,...
our @EXPORT = (qw(
    run_tests
    run_soap_tests
    run_xmlrpc_tests
    base_url
    Dumper
),
@Test::More::EXPORT,
@Test::Exception::EXPORT,
@Test::Deep::EXPORT,
);

our $lwp_opt ||= {timeout => 5};

# Использовать done_testing, опредленный в файле с набором тестов, вместо указания параметра checks_num
our $USE_DONE_TESTING //= 0;

=head2 run_*tests (run_tests, run_soap_tests, run_xmlrpc_tests)

    Параметры позиционные: 
    $tests -- ссылка на массив тестовых сценариев

    Формат сценария:
    {
        # имя сценария, используется для именования отдельных проверок
        name => 'get-user-roles', 
        
        # url, который надо запросить
        url => "$base_url/dostup/get-user-roles", 
        
        # для http-тестов -- http-метод (HEAD, GET, POST)
        # для soap-тестов -- soap-метод
        method => 'GET', 
        method => 'UpdatePrices',
        
        # (необязательно) какой код ответа ожидается
        # Если не указано -- подразумевается 200
        # только для простых http-тестов
        code => 200,
        
        # ссылка на массив наборов данных, 
        # сценарий будет проверен на каждом наборе
        data => [ 'msa', 'lena-san', 'zhur', 'mirage', 'kiev', 'eml' ],
        
        # (необязательно) 
        # функция, получающая на вход набор данных из data, 
        # и возвращающая ссылку на массив/хеш -- форму, 
        # которую надо отправить на указанный url.
        # Если не указана -- будут отправляться данные из data как есть
        preprocess => sub {return {token => $token, login => $_[0]}},
        
        # количество проверок в функции check
        check_num => 4, 
        
        # этот тест описывает back to back тестирование(будет выполнено два запроса на один вызов check)
        is_backtoback => 1
        
        # функция для восстановления состояния для back to back тестирования
        # можно использовать если метод пишущий и результат повторного вызова
        # этого же метода вернет отличный от первого результат
        # при back-to-back тестировании следует запускать тесты на одной и той же среде(одной и той же БД)
        restore_state => sub {}

        # Проверяющая функция. 
        # Получает параметры: 
        #   * набор данных из data, который использовался для запроса;
        #   * объект HTTP::Response;
        #   * имя запроса (имя сценария + номер данных)
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{code}, 0, "$name: error code");
            is(ref $v->{roles}, "ARRAY", "$name: array of roles");
        },
    },

=cut

my %common_checks = (
    http => 1, 
    soap => 2,
    xmlrpc => 2, 
);
sub run_tests_general
{
    my ($type, $tests) = @_;
    
    die 'defined readonly test with restore state' if grep {$_->{restore_state} && $_->{read_only}} @$tests;
    
    # . Считаем тесты
    my @allowed_tests = grep {
        (!$ENV{TEST_INTAPI_BTB_URL} ? !$_->{is_backtoback} : 1)
            && ($ENV{TEST_INTAPI_WRITE_ALLOW} || $_->{read_only})  
    } @$tests;
    my $tests_count = (sum map {
        (($_->{check_num}||0)
            + ($ENV{TEST_INTAPI_BTB_URL} && $_->{is_backtoback} ? $common_checks{$type} * 2 : $common_checks{$type}))
                * @{$_->{data} || []}
    } @allowed_tests) || 0;
    
    if ( @$tests > 0 && @allowed_tests == 0){
        warn "\nno read-only checks in test!\n";
        Test::More::plan(tests => 1);
        is(1, 1, "fake check instead of rw-test");
        return;
    }

    Test::More::plan(tests => $tests_count) if !$USE_DONE_TESTING;

    my $i = 0;
    my $global_data = {};
    for my $case ( @allowed_tests ){
        # Для каждого сценария
        my $j = 0;
        prepare_global_data($type, $global_data, $case);
        for my $data ( @{$case->{data} || []} ){
            # для каждого набора данных
            # собрать имя: имя сценария + номер данных
            my $name = ($case->{name}||$i)."-$j";
            run_single_test($type, $global_data, $case, $data, $name);
            $j++;
        }
        $i++;
    }
    return;
}

sub run_single_test
{
    my ($type, $G, $case, $data, $name) = @_;

    if ($type eq 'http'){
        ### Просто http-запрос

        # сделать запрос;
        my $form = exists $case->{preprocess} ? $case->{preprocess}->($data) : $data;
        if ( $ENV{TEST_INTAPI_VERBOSE} ){
            print STDERR _request_explication($case, $form, $name);
        }
        
        my $schema = $case->{schema};
        my @base_urls = (base_url($schema => $ENV{TEST_INTAPI_BASE_URL}));
        # back to back testing
        if ($ENV{TEST_INTAPI_BTB_URL} && $case->{is_backtoback}) {
            push @base_urls, base_url($schema => $ENV{TEST_INTAPI_BTB_URL});
        }
        
        my @responses;
        for my $base (@base_urls) {
            $case->{restore_state}->($form) if $case->{is_backtoback} && $case->{restore_state};
            my $url = $case->{url} =~ /^http/ ? $case->{url} : $base . $case->{url};
            local $ENV{PERL_LWP_SSL_VERIFY_HOSTNAME} = 0;
            my $resp = submit_form($case->{method}, $url, $form, %$lwp_opt, %{$case->{lwp_opt}||{}});
            is( $resp->code, $case->{code} || 200, "$name: response code" );
            push @responses, $resp;
        }

        # данные, результат и имя передать в check
        if ($case->{check}) {
            $case->{check}->($data, @responses > 1 ? \@responses : $responses[0], $name);
        }

    } elsif ($type eq 'soap') {
        ### soap-запрос

        # сделать запрос;
        my $params = exists $case->{preprocess} ? $case->{preprocess}->($data) : $data;
        if ( $ENV{TEST_INTAPI_VERBOSE} ){
            #print STDERR _request_explication($case, $form, $name);
        }

        my $resp = eval{
            $G->{soap}->call($case->{method} => @$params);
        };
        ok($resp && !$@, "$name: connect");
        ok($resp && !$resp->fault, "$name: success");

        # данные, результат и имя передать в check
        if ($case->{check}) {
            $case->{check}->($data, $resp, $resp->result(), $name);
        }

    } elsif ($type eq "xmlrpc") {
        # xmlrpc- запрос
        
        my $params = exists $case->{preprocess} ? $case->{preprocess}->($data) : $data;
        if ( $ENV{TEST_INTAPI_VERBOSE} ){
            #???
        }
        my $resp = eval { $G->{rpc}->call($case->{method}, @$params) };

        ok($resp && !$@, "$name connect, error: $@");
        if ($case->{fault}){
            ok($resp && $resp->fault, "$name fault: error expected");
        } else {
            ok($resp && !$resp->fault, "$name success, error: ".Dumper($resp->fault));
        }

        if ($case->{check}) {
            $case->{check}->($data, $resp, $name);
        }

    } else {
        die "unknown type $type";
    }

    return;
}

sub prepare_global_data
{
    my ($type, $G, $case) = @_;

    if ($type eq "http"){
        # nothing
    } elsif ($type eq "soap"){
        $G->{soap} = SOAP::Lite->proxy( $case->{proxy}, timeout => 10 );
        $G->{soap}->uri($case->{uri});
    } elsif ($type eq "xmlrpc"){
        $G->{rpc} = XMLRPC::Lite->proxy($case->{proxy}, timeout => 10)
        -> serializer( new Yandex::XMLRPC::UTF8Serializer('utf8') )
        -> deserializer( new Yandex::XMLRPC::UTF8Deserializer('utf8') );
    } else {
        die "unknown type '$type'";
    }

    return;
}


sub run_tests
{
    run_tests_general("http", @_);
}


sub run_soap_tests
{
    run_tests_general("soap", @_);
}

sub run_xmlrpc_tests
{
    run_tests_general("xmlrpc", @_);
}


sub base_url
{
    my ($schema, $url) = @_;
    return ($schema||"http").":".($url || $ENV{TEST_INTAPI_BASE_URL} || '//8804.beta.direct.yandex.ru');
}

sub _request_explication
{
    my ($case, $form, $name) = @_;

    my $url = $case->{method} =~ /^(GET|HEAD)$/ ? Yandex::HTTP::make_url($case->{url}, $form) : $case->{url}; 
    my $h = {
        name => ((caller())[1]||'unknown file').': '.$name,
        method => $case->{method},
        url => $url,
    };

    if ( $case->{method} eq 'POST' ){
        $h->{post_data} = ref $form ? Yandex::HTTP::make_url('', $form) : $form; 
    }

    my $example = "wget " . ($case->{method} eq 'POST' ? " --post-data='$h->{post_data}'" : '')." -qO - '$url'";

    local $Data::Dumper::Indent = 1;
    local $Data::Dumper::Terse = 1;
    local $Data::Dumper::Quotekeys = 0;
    local $Data::Dumper::Sortkeys = 1;

    return decode('utf8', Dumper($h)."$example\n\n");
}

1;
