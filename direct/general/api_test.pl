#!/usr/bin/perl

use my_inc "../..";


=pod

    Скрипт для тестирования создания объявлений через API
        предпочтительно запускать на тестовой, либо разработческой среде
        
    $Id$

=cut

use warnings;
use strict;


#use lib '/var/www/beta.mirage.8035/protected';

use SOAP::Lite;
use Crypt::SSLeay;

use YAML::Syck;
use JSON::Syck;
use Time::HiRes;
use Text::Iconv;
use Data::Dumper;

use Yandex::DBTools;
use Settings;
use ScriptHelper;

use API::Errors;

my $conv = new Text::Iconv('cp1251', 'utf8');
$YAML::Syck::ImplicitUnicode = 1;

=head2 Авторизация скрипта

    Авторизация осуществляется на основе сертификата, выданного Центром Сертификации Яндекс.Директ
    Для этого необходимо инициализировать следующие переменные окружения:

        HTTPS_CERT_FILE - абсолютный путь к пользовательскому сертификату, подписанному Центром Сертификации Яндекс.Директ,
        HTTPS_KEY_FILE - абсолютный путь к файлу с секретным ключом,
        HTTPS_CA_FILE - абсолютный путь к Директ CA файлу, которым подписан пользовательский сертификат,
        HTTPS_CA_DIR - абсолютный путь в каталогу с CA сертификатами.

=cut

# тестовое агентство mirage-agency4
#$ENV{HTTPS_CERT_FILE} = '/home/mirage/certs/mirage-agency4/cert.crt';
#$ENV{HTTPS_KEY_FILE}  = '/home/mirage/certs/mirage-agency4/private.key';
#$ENV{HTTPS_CA_FILE}   = '/home/mirage/certs/mirage-agency4/cacert.pem';
#$ENV{HTTPS_CA_DIR}    = '/home/mirage/certs/mirage-agency4/';

my $login = 'mirage-agency4';
my $token = '513e52e2dc241200411d6fd11c9137f4';

my ($cmd, $beta, $debug) = ('PingAPI', undef, undef);

extract_script_params(
    'cmd=s' => \$cmd,
    'beta' => \$beta,
    'debug' => \$debug,
);

my $soap_options = {
    debug => $debug,
    beta => $beta,
};


# заполняем заголовок
my $nowtime = time();
my @headers = (
    SOAP::Header->name("token")->value($token)->type("")
    , SOAP::Header->name("login")->value($login)->type("")
    , SOAP::Header->name("eventtime")->value($nowtime)->type("")
);

$soap_options->{headers} = \@headers;

our @required = (
    'Text', 'Title', 'CampaignID', 'Phrases'
);

our @required_contacts = (
    'Country', 'City', 'Phone', 'CompanyName', 'WorkTime'
);

my $BANNER = {
        CampaignID => 1400097,
        BannerID => 0,
        'Text' => $conv->convert("testestestestes api 2"),
        Title => $conv->convert("test api 1"),        
        Href => "ya.ru",
        Geo => '1,2,213,214',
        Phrases => [
            {
                Phrase => "phrase test 1",
                IsRubric => 0,
                AutoBroker => 1,
                Price => 0.13,
            },
        ],
        ContactInfo => {
            Country => $conv->convert("Россия"),
            City => $conv->convert("Москва"),
            CityCode => 499,
            CountryCode => "+7",
            Phone => "777-33-33",
            Street => $conv->convert("Самокатная"),
            House => 1,
            Build => 21,
            CompanyName => $conv->convert("Яндекс"),
            ContactPerson => $conv->convert("Виктория"),
            WorkTime=> "0#6#9#00#18#00",
            IMLogin => "test",
            IMClient => "icq",
            ExtraMessage => "testestestestestestsete",
        }
};

check_overview_methods();

#######
my %banner = %$BANNER;
check_call_unit('full', \%banner, 1);

#die "Test";

#######
delete $banner{ContactInfo};
check_call_unit('without contacts', \%banner, 1);

#######
%banner = %$BANNER;
delete $banner{Href};
check_call_unit('without href', \%banner, 1);

#######
%banner = %$BANNER;
delete $banner{Href};
delete $banner{ContactInfo}{Phone};
check_call_unit('href and phone is empty', \%banner, 0, 'BadParams');

#######
%banner = %$BANNER;
delete $banner{IMClient};
check_call_unit('im client empty', \%banner, 0);

#######
foreach my $cn (@required) {
    my %banner = %$BANNER;
    delete $banner{$cn};
    check_call_unit("$cn empty", \%banner, 0, 'BadParams');
}

#######
foreach my $cn (@required_contacts) {
    my %banner = %$BANNER;
    delete $banner{ContactInfo}{$cn};
    check_call_unit("$cn empty", \%banner, 0, 'BadParams');
}

# тестирование стандартных методов "для чтения"
# PingAPI -> GetClientsList => GetClientInfo -> GetCampaignsList => GetBalance, GetBanners => GetBannerPhrases => UpdatePrices

sub check_overview_methods
{
    if (check_method('PingAPI', undef, $soap_options)) {
        my $clients = check_method('GetClientsList');
        foreach my $client (@$clients) {
            my $client_info = check_method('GetClientInfo', [$client->{Login}]);
            
            my $camps = check_method('GetCampaignsList', [$client->{Login}]);
            foreach my $camp (@$camps) {
                
                my $balance = check_method('GetBalance', [$camp->{CampaignID}]);
                
                my $banners = check_method('GetBanners', {CampaignIDS => [$camp->{CampaignID}]});                
                foreach my $banner (@$banners) {
                    
                    my $phrases = check_method('GetBannerPhrases', [$banner->{BannerID}]);
                    foreach my $phrase (@$phrases) {
                        # чего-нибудь проверяем в фразах
                    }
                }
            }
        }
    }
}

sub check_method
{
    my ($method, $data, $soap_options) = @_;
    
    my ($result, $code, $string, $details) = call_method($method, $data, $soap_options);

    my $json_data = JSON::Syck::Dump($data);    
    if ($code) {
        print STDERR "$method ($json_data) - bad\n";
        die "Error ($method, $code): $string ($details), ".Dumper {data => $data};
    } else {
        print STDERR "$method ($json_data) - ok\n";
    }
    
    return $result;
}

sub check_call_unit
{
    my $desc = shift;
    my $data = shift;
    my ($need_result, $need_code_name) = @_;
    
    my %err = %API::Errors::ERRORS;
    my $need_code = $need_code_name ? $err{$need_code_name}->{code} : 0;
    
    my ($result, $code, $string, $details) = call_method('CreateOrUpdateBanners', [$data], $soap_options);
    
    my $result_flag = $result && $result->[0] ? 1 : 0;
    $code =~ s/\D//gsi if defined $code;
    
    if ($need_code_name && defined $need_result && !$need_result
            && !$result_flag) {
                
        # нужно проверить еще и номер ошибки
        if ($code ne $need_code) {
            print "$desc: failed! (code:$code, need:$need_code)\n";
            return 0;
        }
    }
    
    # если команда выполнилась успешно
    if ($result_flag eq $need_result) {
        
        my $r = check_values($result, $data);
        if ($r) {
            print "$desc: $r - not right column value! (bid: $result)\n";
        }
        
        if ($result->[0]) {
            print call_method('DeleteBanners', {CampaignID => $data->{CampaignID}, BannerIDS => [$result->[0]]}, $soap_options);
        }
        print "ok\n";
        return 1;
    } else {
        print "$desc: failed! (result: ".$result->[0].")\n";
        return 0;
    }
}

sub check_exists_banner
{
    my $bid = shift;
    
    return get_one_field_sql(PPC(bid => $bid), "select count(*) from banners b inner join phrases using(pid) where b.bid = ?", $bid);
}

sub check_values
{
    my $bid = shift;
    my $values = shift;
    
    foreach my $k (keys %{$values->{ContactInfo}}) {
        $values->{$k} = $values->{ContactInfo}->{$k};
    }
    
    my $banner = get_one_line_sql(PPC(bid => $bid), "select * from banners b left join phrases p using(pid) where b.bid = ?", $bid);
    foreach my $key (keys %$banner) {
        if (exists $values->{lc $key}) {
            return $key if $values->{$key} ne $banner->{$key};
        }
    }
    
    return undef;
}

sub call_method
{
    my $method_name = shift || return;
    my $method_data = shift;
    my $soap_options = shift;

    my $ltm = Time::HiRes::time();
    #print "Call method: $method_name\n";

    my $soap = SOAP::Lite
        -> uri('API');

    if (defined $soap_options->{debug}) {
        # $soap -> on_debug(sub {print join(">\n", split(">", join("", @_)));});
        $soap -> on_debug(sub {print @_});
    }

    if ($soap_options->{beta}) {
        #$soap -> proxy('https://beta.direct.yandex.ru:8092/api-1.0');
        $soap -> proxy('https://beta.direct.yandex.ru:8036/api-1.0');        
    } else {
        $soap -> proxy('https://soap-new.direct.yandex.ru/api-1.0');
        #$soap -> proxy('https://ppcsoap01c.yandex.ru/api-1.0');
        #$soap -> proxy('https://soap-new.direct.yandex.ru/api');
    }

    my $request = $soap->call($method_name, @{ $soap_options->{headers}|| []}, $method_data);
    #printf "(time: %0.4f sec)", Time::HiRes::time() - $ltm; print "\n";
    unless ($request->fault) {
        # в случае успешного выполнения запроса - возвращаем результат
        return $request->result;            
    } else {
        # если возникла ошибка - выводим сообщение в STDERR и завершаем выполнение программы
        return (undef, $request->faultcode, $request->faultstring, $request->faultdetail);
    }
}
