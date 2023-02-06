package API::Test::Reports::Xsd;
use Direct::Modern;

=head1 NAME

API::Test::Reports::Xsd

=head1 DESCRIPTION

Правильно шаблонизировать шаблон reports.xsd, а потом из результирующего
файла достать значения в каком-нибудь enum.

=cut

use parent 'Exporter';

our @EXPORT_OK = qw( get_enum_values );

use Template;
use XML::Compile::Schema;

use Settings;

my @XSD_NAMES = qw( reports general );

my %XSD_NAME_TO_FILENAME = (
    'reports' => "$Settings::ROOT/api/wsdl/v5/reports.xsd",
    'general' => "$Settings::ROOT/api/wsdl/v5/general.xsd",
);

my %XSD_NAME_TO_NAMESPACE = (
    'reports' => 'http://api.direct.yandex.com/v5/reports',
    'general' => 'http://api.direct.yandex.com/v5/general',
);

my $template = Template->new( ABSOLUTE => 1 );

my $schema = XML::Compile::Schema->new;
for my $xsd_name (@XSD_NAMES) {
    my $xsd_content;
    $template->process($XSD_NAME_TO_FILENAME{$xsd_name}, {
        api_version => 5,
        is_beta => 0,
        lang => 'ru',
        API_SERVER_PATH => "https://api.direct.yandex.ru",
    }, \$xsd_content);

    $schema->importDefinitions(\$xsd_content);
}

=head2 get_enum_values('reports|general', 'SomeEnum')

Вернуть список допустимых значений в enum. Возвращает список.

=cut

sub get_enum_values {
    my ($xsd_name, $enum_name) = @_;
    my $ns = $XSD_NAME_TO_NAMESPACE{$xsd_name};
    my $enum_element = $schema->namespaces->find('simpleType' => "{$ns}$enum_name")->{node};

    my @values;
    for my $value_element ( $enum_element->getElementsByTagName("xsd:enumeration") ) {
        push @values, $value_element->getAttribute('value');
    }

    return sort @values;
}

1;
