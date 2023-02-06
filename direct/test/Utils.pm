package API::Test::Utils;
use Direct::Modern;

=pod

    $Id$

=head1 NAME

    API::Test::Utils — набор функций, часто используемых в модульных тестах API

=head1 DESCRIPTION

   Функции, часто используемые при написании модульных тестов для сервисов API.

=cut

use Test::Deep;

use Exporter qw/import/;

our @EXPORT_OK = qw/
    get_campaign_info_map_from_db_mock_info
    get_error_with_ignored_description
/;

=head2 get_error_with_ignored_description($error)

    Возвращает объект ошибки для сравнения без учета детального описания.

=cut

sub get_error_with_ignored_description {
    my ($error) = @_;
    $error->{description} = ignore();
    return $error;
}

=head2 get_campaign_info_map_from_db_mock_info

    Функция формирует набор данных для валидации допустимых типов кампаний в сервисах API5

=cut

sub get_campaign_info_map_from_db_mock_info {
    my %options = @_;
    return [
        {
            args => ignore(),
            result => {
                map {
                    $_ => { cid => $_, uid => $options{uid}, type => 'text', archived => $options{archived} // 'No', source => 'api', currency => 'rub' }
                } exists $options{cids} ? @{$options{cids}} : exists $options{cid} ? $options{cid} : ()
            }
        }
    ];
}


1;
