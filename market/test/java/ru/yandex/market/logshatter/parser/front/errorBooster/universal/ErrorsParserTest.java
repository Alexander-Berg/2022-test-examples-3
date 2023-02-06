package ru.yandex.market.logshatter.parser.front.errorBooster.universal;


import java.util.Arrays;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.logshatter.sanitizer.FakeSanitizer;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.LogLevel;
import ru.yandex.market.logshatter.parser.front.errorBooster.Parser;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;
import ru.yandex.market.logshatter.parser.front.errorBooster.Runtime;

public class ErrorsParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new ErrorsParser(new FakeSanitizer()));
        checker.setLogBrokerTopic("universal-error-topic");
    }

    @Test
    public void minimalFieldsParser() throws Exception {
        String line = "{\"message\":\"Column not found: 1\",\"project\":\"error-booster\",\"timestamp\":1571747133800}";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1571747133000L),
            "error-booster", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "", // REQUEST_ID
            hashOfEmptyString, // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            "Column not found: 1", // MESSAGE
            UnsignedLong.valueOf("4785717824185780193"), // MESSAGE_ID
            "Column not found: 1", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.UNKNOWN, // RUNTIME
            LogLevel.UNKNOWN, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            "", // STACK_TRACE
            hashOfEmptyString, // STACK_TRACE_ID
            "", // ORIGINAL_STACK_TRACE
            Arrays.asList(), // STACK_TRACE_NAMES
            Arrays.asList(), // STACK_TRACE_URLS
            Arrays.asList(), // STACK_TRACE_LINES
            Arrays.asList(), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.UNIVERSAL, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void fullFieldsParser() throws Exception {
        String line = "{\"stack\":\"Error: Column not found: 1\\n    at ErrorQueries.getGroupedList " +
            "(/app/modules/error_queries/module.js:1517:19)\\n    at app.get (/app/app.js:191:22)\\n    at Layer" +
            ".handle [as handle_request] (/app/node_modules/express/lib/router/layer.js:95:5)\",\"dc\":\"vla\"," +
            "\"level\":\"error\",\"slots\":\"105520,0,79;103926,0,34\",\"source\":\"source\"," +
            "\"sourceMethod\":\"source_method\",\"isInternal\":\"true\",\"isRobot\":\"true\"," +
            "\"sourceType\":\"source_type\",\"fingerprint\":\"mysql error aaaa 123 bbbb\"," +
            "\"host\":\"error-booster-api-1.vla.yp-c.yandex.net\",\"file\":\"file\",\"message\":\"Column not found: " +
            "1\",\"project\":\"error-booster\",\"line\":\"1931231\",\"col\":\"1231312\",\"service\":\"service\"," +
            "\"ip\":\"192.168.1.1\",\"experiments\":\"aaa=1;bb=7;ccc=yes;ddd\",\"reqid\":\"1571759797.76789.122049" +
            ".2619\",\"platform\":\"desktop\",\"loggedin\":true,\"method\":\"method\"," +
            "\"block\":\"block_WITH_VERY_VERY_SECRET_\",\"language\":\"nodejs\",\"region\":\"199\",\"version\":\"1.0" +
            ".0\",\"yandexuid\":\"1231312352322323\",\"env\":\"production\",\"useragent\":\"Mozilla/5.0 (Macintosh; " +
            "Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826.0 Safari/537.36\"," +
            "\"timestamp\":1571747133800,\"url\":\"https://api.error.yandex-team" +
            ".ru/api/grouped_list?field=1&baseFilter={%22project%22:%22web4%22}&limit=10&table=errors\"," +
            "\"additional\":{\"login\":\"anonymous\",\"hostname\":\"error-booster-api-1.sas.yp-c.yandex.net\"}," +
            "\"page\":\"/api/grouped_list\"}";

        String stacktrace = "Error: Column not found: 1\n" +
            "    at ErrorQueries.getGroupedList (/app/modules/error_queries/module.js:1517:19)\n" +
            "    at app.get (/app/app.js:191:22)\n" +
            "    at Layer.handle [as handle_request] (/app/node_modules/express/lib/router/layer.js:95:5)";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1571747133000L),
            "error-booster", // PROJECT
            "service", // SERVICE
            "/api/grouped_list", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://api.error.yandex-team.ru/api/grouped_list?field=1&baseFilter={%22project%22:%22web4%22}&limit=10" +
                "&table=errors", // URL
            UnsignedLong.valueOf("16962607284107015865"), // URL_ID
            "api.error.yandex-team.ru", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(103926, 105520), // TEST_IDS
            Arrays.asList("aaa=1", "bb=7", "ccc=yes", "ddd"), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BfROWSER_ENGINE_VERSION
            "YandexBrowser", // BROWSER_NAME
            "18.9.0.3363", // BROWSER_VERSION
            "18.9", // BROWSER_VERSION_MAJOR
            "Chromium", // BROWSER_BASE
            "MacOS", // OS_FAMILY
            "Mac OS X Sierra", // OS_NAME
            "10.12.6", // OS_VERSION
            "10.12", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            true, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "1.0.0", // VERSION
            199, // REGION
            "1571759797.76789.122049.2619", // REQUEST_ID
            UnsignedLong.valueOf("6340909726585862471"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("1231312352322323"), // YANDEXUID
            Arrays.asList("login", "hostname"), // KV_KEYS
            Arrays.asList("anonymous", "error-booster-api-1.sas.yp-c.yandex.net"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:192.168.1.1", // IPv6
            true, // LOGGED_IN
            "Column not found: 1", // MESSAGE
            UnsignedLong.valueOf("17273934179559887257"), // MESSAGE_ID
            "Column not found: 1", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "file", // FILE
            UnsignedLong.valueOf("17518722355242166903"), // FILE_ID
            "block_WITH_VERY_VERY_SECRET_", // BLOCK
            "method", // METHOD
            1931231, // LINE
            1231312, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("1753300842284806854"), // STACK_TRACE_ID
            stacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("ErrorQueries.getGroupedList", "app.get", "Layer.handle [as handle_request]"), //
            // STACK_TRACE_NAMES
            Arrays.asList("/app/modules/error_queries/module.js", "/app/app.js", "/app/node_modules/express/lib" +
                "/router/layer.js"), // STACK_TRACE_URLS
            Arrays.asList(1517, 191, 95), // STACK_TRACE_LINES
            Arrays.asList(19, 22, 5), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3826" +
                ".0 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("1319564247142717470"), // USER_AGENT_ID
            "source", // SOURCE
            "source_method", // SOURCE_METHOD
            "source_type", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.UNIVERSAL, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "error-booster-api-1.vla.yp-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void goFieldsParser() throws Exception {
        String line = "{\"message\":\"some error\",\"timestamp\":1571747133800,\"project\": \"morda\"," +
            "\"language\":\"go\",\"reqid\":\"1586886844.57099.12345.785749\",\"level\":\"error\"," +
            "\"parsed_stacktrace\":[{\"file\":\"contrib/go/_std/src/runtime/debug/stack.go\"," +
            "\"function\":\"runtime/debug.Stack\",\"line\":24},{\"file\":\"portal/morda-go/internal/utils/stack.go\"," +
            "\"function\":\"a.yandex-team.ru/portal/morda-go/internal/utils.getParsedStack\",\"line\":115}]," +
            "\"yandexuid\":null,\"additional\":null,\"block\":\"block_WITH_VERY_VERY_SECRET_\"}";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");
        checker.setParam("sanitizer", "true");

        checker.check(
            line,
            new Date(1571747133000L),
            "morda", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "1586886844.57099.12345.785749", // REQUEST_ID
            UnsignedLong.valueOf("5176620268014872415"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            "some error", // MESSAGE
            UnsignedLong.valueOf("6803277740514449401"), // MESSAGE_ID
            "some error", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.GO, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "block_WITH_VERY_VERY_XXXXXX_", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            "", // STACK_TRACE
            UnsignedLong.valueOf("11409993852681191577"), // STACK_TRACE_ID
            "", // ORIGINAL_STACK_TRACE
            Arrays.asList("runtime/debug.Stack", "a.yandex-team.ru/portal/morda-go/internal/utils.getParsedStack"),
            // STACK_TRACE_NAMES
            Arrays.asList("contrib/go/_std/src/runtime/debug/stack.go", "portal/morda-go/internal/utils/stack.go"),
            // STACK_TRACE_URLS
            Arrays.asList(24, 115), // STACK_TRACE_LINES
            Arrays.asList(0, 0), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.UNIVERSAL, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void perlFieldsParser() throws Exception {
        String line = "{\"message\":\"some error\",\"timestamp\":1571747133800,\"project\": \"partner\"," +
            "\"language\":\"perl\",\"reqid\":\"1586886844.57099.12345.785749\",\"level\":\"error\"," +
            "\"parsed_stacktrace\":[{\"function\":\"QBit::WebInterface::FastCGI::run\",\"pre_context\":[\"\\n\",\"my " +
            "$web_interface = $ARGV[0]->new();\\n\",\"\\n\",\"$proc_manager->pm_manage();\\n\",\"\\n\",\"my $n = 0;" +
            "\\n\",\"while ($request->Accept() >= 0) {\\n\",\"    $proc_manager->pm_pre_dispatch();\\n\",\"    " +
            "$ENV{FORCE_GETTEXT_XS} //= $init_force_gettext_xs;\\n\",\"    %ENV = (%ENV, %saved_env);\\n\"]," +
            "\"vars\":{\"@_\":[\"IntAPI=HASH(0x18c8b50)\",\"FCGI=SCALAR(0x188f7f0)\"]},\"post_context\":[\"    " +
            "$proc_manager->pm_post_dispatch();\\n\",\"    exit(0) if $args{'max-requests'} && ++$n >= " +
            "$args{'max-requests'};\\n\",\"}\\n\",\"\\n\",\"FCGI::CloseSocket($socket);\\n\",\"\\n\",\"__END__\\n\"," +
            "\"\\n\",\"=head1 NAME\\n\",\"\\n\"],\"context_line\":\"    $web_interface->run($request);\\n\"," +
            "\"filename\":\"/usr/share/partners/bin/qbit_fcgi_starter\",\"lineno\":\"100\",\"module\":\"main\"}," +
            "{\"function\":\"IntAPI::build_response\",\"pre_context\":[\"      );\\n\",\"}\\n\",\"\\n\",\"sub run " +
            "{\\n\",\"    my ($self, $r) = @_;\\n\",\"\\n\",\"    $self = $self->new() unless blessed($self);\\n\"," +
            "\"\\n\",\"    $self->request(QBit::WebInterface::FastCGI::Request->new(request => $r));\\n\",\"\\n\"]," +
            "\"vars\":{\"@_\":[\"IntAPI=HASH(0x18c8b50)\"]},\"post_context\":[\"\\n\",\"    my $data_ref = " +
            "\\\\$self->response->data;\\n\",\"\\n\",\"    if (defined($data_ref)) {\\n\",\"        $data_ref = " +
            "$$data_ref if ref($$data_ref);\\n\",\"\\n\",\"        if (ref($data_ref) eq 'SCALAR') {\\n\",\"         " +
            "   if (defined($$data_ref)) {\\n\",\"                utf8::encode($$data_ref) if utf8::is_utf8" +
            "($$data_ref);\\n\",\"            } else {\\n\"],\"context_line\":\"    $self->build_response();\\n\"," +
            "\"filename\":\"/usr/share/partners/lib/QBit/WebInterface/FastCGI.pm\",\"lineno\":\"82\"," +
            "\"module\":\"QBit::WebInterface::FastCGI\"},{\"function\":\"QBit::HTTPAPI::build_response\"," +
            "\"pre_context\":[\"            if ($_[0]->{'result'} eq 'ok') {\\n\",\"                return " +
            "\\\\to_json($_[0]->{'data'}, pretty => $_[1]);\\n\",\"            } elsif ($_[0]->{'result'} eq 'error')" +
            " {\\n\",\"                return \\\\to_json($_[0], pretty => $_[1]);\\n\",\"            } else {\\n\"," +
            "\"                throw gettext('Unknown error type \\\"%s\\\"', $_[0]->{'result'});\\n\",\"            " +
            "}\\n\",\"        },\\n\",\"    };\\n\",\"\\n\"],\"vars\":{\"@_\":[\"IntAPI=HASH(0x18c8b50)\"]}," +
            "\"post_context\":[\"}\\n\",\"\\n\",\"return TRUE;\\n\"],\"context_line\":\"    return " +
            "$self->SUPER::build_response();\\n\",\"filename\":\"/usr/share/partners/lib/IntAPI.pm\"," +
            "\"lineno\":\"81\",\"module\":\"IntAPI\"},{\"function\":\"IntAPI::Method::Form::update_contract\"," +
            "\"pre_context\":[\"                $params{$param} = $value if defined($value);\\n\",\"            " +
            "}\\n\",\"\\n\",\"            $api->pre_run($method, \\\\%params);\\n\",\"\\n\",\"            if (exists" +
            "($attrs->{'formats'}) && !grep {$format eq $_} @{$attrs->{'formats'}}) {\\n\",\"                throw " +
            "Exception::Validation gettext('Supported only this formats: %s',\\n\",\"                    join(', ', " +
            "@{$attrs->{'formats'}}));\\n\",\"            }\\n\",\"\\n\"]," +
            "\"vars\":{\"@_\":[\"IntAPI::Method::Form=HASH(0xf3b6550)\"]},\"post_context\":[\"\\n\",\"            " +
            "$self->response->content_type($SERIALIZERS{$format}->{'content_type'});\\n\",\"\\n\",\"            if " +
            "($attrs->{STREAM}) {\\n\",\"                throw 'Not supported format' unless " +
            "$STREAM_SERIALIZERS{$format};\\n\",\"\\n\",\"                # по умолчанию мы ставим end_marker, только" +
            " если явно не указано обратное\\n\",\"                # предполагается, что в будущем всегда будет " +
            "end_marker, и атрибут можно будет убрать\\n\",\"                my $end_marker;\\n\",\"                " +
            "if ($attrs->{NO_END_MARKER}) {\\n\"],\"context_line\":\"            my $ref = " +
            "$methods->{$path}{$method}{'sub'}($api, %params);\\n\"," +
            "\"filename\":\"/usr/share/partners/lib/QBit/HTTPAPI.pm\",\"lineno\":\"124\"," +
            "\"module\":\"QBit::HTTPAPI\"},{\"function\":\"QBit::Application::Model::API::Yandex::Balance" +
            "::update_contract\",\"pre_context\":[\"                contract_id  => {type => 'int_un',},\\n\",\"     " +
            "       },\\n\",\"            extra => TRUE,\\n\",\"        },\\n\",\"        throw => 1,\\n\",\"    );" +
            "\\n\",\"\\n\",\"    my $contract;\\n\",\"    my $error_message;\\n\",\"    try {\\n\"]," +
            "\"vars\":{\"@_\":[\"Application::Model::API::Yandex::Balance=HASH(0xdf32800)\",\"start_dt\"," +
            "\"2021-03-02\",\"person_id\",\"13266310\",\"contract_id\",\"2507031\",\"operator_uid\",\"1251917335\"]}," +
            "\"post_context\":[\"    }\\n\",\"    catch {\\n\",\"        my $exception = shift;\\n\",\"\\n\",\"      " +
            "  # Для простоты и удобства разработки мы отдаем информацию про ошибку в ответе ручки\\n\",\"        " +
            "$error_message = $exception->message();\\n\",\"\\n\",\"        # Но так же и формируем файл с описанем " +
            "ощибки, чтобы видеть его в почте\\n\",\"        $self->exception_dumper->dump_as_html_file($exception);" +
            "\\n\",\"    };\\n\"],\"context_line\":\"        $contract = $self->api_balance->update_contract(%$opts);" +
            "\\n\",\"filename\":\"/usr/share/partners/lib/IntAPI/Method/Form.pm\",\"lineno\":\"237\"," +
            "\"module\":\"IntAPI::Method::Form\"},{\"function\":\"QBit::Application::Model::API::Yandex::Balance" +
            "::call\",\"pre_context\":[\"\\n\",\"sub update_contract {\\n\",\"    my ($self, %opts) = @_;\\n\"," +
            "\"\\n\",\"    throw \\\"Expected 'operator_uid'\\\" unless defined $opts{operator_uid};\\n\",\"    throw" +
            " \\\"Expected 'contract_id'\\\"  unless defined $opts{contract_id};\\n\",\"\\n\",\"    my $operator_uid " +
            "= delete $opts{operator_uid};\\n\",\"    my $contract_id  = delete $opts{contract_id};\\n\",\"\\n\"]," +
            "\"vars\":{\"@_\":[\"Application::Model::API::Yandex::Balance=HASH(0xdf32800)\",\"Balance2" +
            ".UpdateContract\",\"1251917335\",\"2507031\",{\"start_dt\":\"2021-03-02\",\"person_id\":\"13266310\"}]}," +
            "\"post_context\":[\"\\n\",\"    local $Data::Dumper::Terse = 1;\\n\",\"\\n\",\"    throw " +
            "Exception::Balance::IncorrectAnswer Dumper($result), undef, undef,\\n\",\"      sentry => {fingerprint " +
            "=> ['Balance', 'IncorrectAnswer', 'Not array']}\\n\",\"      if ref $result ne 'ARRAY';\\n\",\"    throw" +
            " Exception::Balance::IncorrectAnswer Dumper($result) if scalar @$result != 1;\\n\",\"\\n\",\"    my " +
            "$contract = $result->[0];\\n\",\"\\n\"],\"context_line\":\"    my $result = $self->call('Balance2" +
            ".UpdateContract', $operator_uid, $contract_id, \\\\%opts);\\n\"," +
            "\"filename\":\"/usr/share/partners/lib/QBit/Application/Model/API/Yandex/Balance.pm\"," +
            "\"lineno\":\"620\",\"module\":\"QBit::Application::Model::API::Yandex::Balance\"}," +
            "{\"function\":\"Exception::throw\",\"pre_context\":[\"            $bad_response = TRUE;\\n\",\"        " +
            "}\\n\",\"\\n\",\"        my $dump_str = $hide_opts ? 'XXX' : substr(to_json(\\\\@opts, pretty => TRUE), " +
            "0, 1000);\\n\",\"        $xml->root->new(opts => $dump_str)->paste(last_child => $xml->root);\\n\"," +
            "\"\\n\",\"        open my $fh, \\\">\\\", \\\\$error;\\n\",\"        $xml->set_pretty_print('indented');" +
            "\\n\",\"        $xml->print($fh);\\n\",\"\\n\"]," +
            "\"vars\":{\"@_\":[\"Exception::Balance::IncorrectAnswer\",\"<error>\\n  <msg>Rule violation: 'Договор на" +
            " плательщика с ИНН 771894810066 на выбранный период уже существует: 2686211'</msg>\\n  " +
            "<wo-rollback>0</wo-rollback>\\n  <rule>Договор на плательщика с ИНН 771894810066 на выбранный период уже" +
            " существует: 2686211</rule>\\n  <method>Balance2.UpdateContract</method>\\n  " +
            "<code>CONTRACT_RULE_VIOLATION</code>\\n  <parent-codes>\\n    <code>EXCEPTION</code>\\n  " +
            "</parent-codes>\\n  <contents>Rule violation: 'Договор на плательщика с ИНН 771894810066 на выбранный " +
            "период уже существует: 2686211'</contents>\\n  <opts>[\\n   \\\"1251917335\\\",\\n   \\\"2507031\\\",\\n" +
            "   {\\n      \\\"person_id\\\" : \\\"13266310\\\",\\n      \\\"start_dt\\\" : \\\"2021-03-02\\\"\\n   " +
            "}\\n]\\n</opts>\\n</error>\\n\",\"undef\",\"undef\",\"sentry\",{\"fingerprint\":[\"Balance\"," +
            "\"IncorrectAnswer\",\"XMLRPC error\"]}]},\"post_context\":[\"          sentry => {fingerprint => " +
            "['Balance', 'IncorrectAnswer', ($bad_response ? 'Bad response' : 'XMLRPC error')]};\\n\",\"    };\\n\"," +
            "\"\\n\",\"    return $return;\\n\",\"}\\n\",\"\\n\",\"=head2 create_client\\n\",\"\\n\",\"B<Параметры:> " +
            "1) $self 2) %opts\\n\",\"\\n\"],\"context_line\":\"        throw Exception::Balance::IncorrectAnswer " +
            "$error, undef, undef,\\n\",\"filename\":\"/usr/share/partners/lib/QBit/Application/Model/API/Yandex" +
            "/Balance.pm\",\"lineno\":\"226\",\"module\":\"QBit::Application::Model::API::Yandex::Balance\"}," +
            "{\"filename\":\"/usr/share/partners/lib/Exception.pm\",\"pre_context\":[\"        package   => " +
            "$caller->{'package'},\\n\",\"        line      => $caller->{'line'},\\n\",\"        callstack => " +
            "\\\\@call_stack,\\n\",\"    };\\n\",\"\\n\",\"    bless $self, $class;\\n\",\"    return $self;\\n\"," +
            "\"}\\n\",\"\\n\",\"sub throw {\\n\"],\"post_context\":[\"}\\n\",\"\\n\",\"1;\\n\"],\"lineno\":\"90\"," +
            "\"context_line\":\"    QBit::Exceptions::throw(shift->new(@_));\\n\",\"module\":\"Exception\"}," +
            "{\"filename\":null,\"function\":null,\"pre_context\":null,\"post_context\":null,\"context_line\":null," +
            "\"module\":null,\"lineno\":null,\"colno\":null}],\"yandexuid\":null,\"additional\":null}";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1571747133000L),
            "partner", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.UNKNOWN, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "1586886844.57099.12345.785749", // REQUEST_ID
            UnsignedLong.valueOf("5176620268014872415"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            "some error", // MESSAGE
            UnsignedLong.valueOf("6803277740514449401"), // MESSAGE_ID
            "some error", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.PERL, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            "", // STACK_TRACE
            UnsignedLong.valueOf("6550304142720443925"), // STACK_TRACE_ID
            "", // ORIGINAL_STACK_TRACE
            Arrays.asList("QBit::WebInterface::FastCGI::run", "IntAPI::build_response", "QBit::HTTPAPI" +
                "::build_response", "IntAPI::Method::Form::update_contract", "QBit::Application::Model::API::Yandex" +
                "::Balance::update_contract", "QBit::Application::Model::API::Yandex::Balance::call", "Exception" +
                "::throw", "(anonymous)"), // STACK_TRACE_NAMES
            Arrays.asList("/usr/share/partners/bin/qbit_fcgi_starter", "/usr/share/partners/lib/QBit/WebInterface" +
                "/FastCGI.pm", "/usr/share/partners/lib/IntAPI.pm", "/usr/share/partners/lib/QBit/HTTPAPI.pm", "/usr" +
                "/share/partners/lib/IntAPI/Method/Form.pm", "/usr/share/partners/lib/QBit/Application/Model/API" +
                "/Yandex/Balance.pm", "/usr/share/partners/lib/QBit/Application/Model/API/Yandex/Balance.pm", "/usr" +
                "/share/partners/lib/Exception.pm"), // STACK_TRACE_URLS
            Arrays.asList(100, 82, 81, 124, 237, 620, 226, 90), // STACK_TRACE_LINES
            Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.UNIVERSAL, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void pythonFieldsParser() throws Exception {
        String line = "{\"stack\":\"timeout: timed out\\n  File \\\"urllib3/connection.py\\\", line 160, in " +
            "_new_conn\\n    (self._dns_host, self.port), self.timeout, **extra_kw)\\n  File " +
            "\\\"urllib3/util/connection.py\\\", line 80, in create_connection\\n    raise err\\n  File " +
            "\\\"urllib3/util/connection.py\\\", line 70, in create_connection\\n    sock.connect(sa)" +
            "\\nConnectTimeoutError: (<urllib3.connection.HTTPConnection object at 0x7f67a5eb0f50>, 'Connection to " +
            "geobase.qloud.yandex.ru timed out. (connect timeout=0.25)')\\n  File \\\"urllib3/connectionpool.py\\\", " +
            "line 603, in urlopen\\n    chunked=chunked)\\n  File \\\"urllib3/connectionpool.py\\\", line 355, in " +
            "_make_request\\n    conn.request(method, url, **httplib_request_kw)\\n  File \\\"http/client.py\\\", " +
            "line 1252, in request\\n    self._send_request(method, url, body, headers, encode_chunked)\\n  File " +
            "\\\"http/client.py\\\", line 1298, in _send_request\\n    self.endheaders(body, " +
            "encode_chunked=encode_chunked)\\n  File \\\"http/client.py\\\", line 1247, in endheaders\\n    self" +
            "._send_output(message_body, encode_chunked=encode_chunked)\\n  File \\\"http/client.py\\\", line 1026, " +
            "in _send_output\\n    self.send(msg)\\n  File \\\"http/client.py\\\", line 966, in send\\n    self" +
            ".connect()\\n  File \\\"urllib3/connection.py\\\", line 183, in connect\\n    conn = self._new_conn()\\n" +
            "  File \\\"urllib3/connection.py\\\", line 165, in _new_conn\\n    (self.host, self.timeout))" +
            "\\nMaxRetryError: HTTPConnectionPool(host='geobase.qloud.yandex.ru', port=80): Max retries exceeded with" +
            " url: /v1/parents?id=65 (Caused by ConnectTimeoutError(<urllib3.connection.HTTPConnection object at " +
            "0x7f67a5eb0f50>, 'Connection to geobase.qloud.yandex.ru timed out. (connect timeout=0.25)'))\\n  File " +
            "\\\"requests/adapters.py\\\", line 449, in send\\n    timeout=timeout\\n  File " +
            "\\\"urllib3/connectionpool.py\\\", line 641, in urlopen\\n    _stacktrace=sys.exc_info()[2])\\n  File " +
            "\\\"urllib3/util/retry.py\\\", line 399, in increment\\n    raise MaxRetryError(_pool, url, error or " +
            "ResponseError(cause))\\nConnectTimeout: HTTPConnectionPool(host='geobase.qloud.yandex.ru', port=80): Max" +
            " retries exceeded with url: /v1/parents?id=65 (Caused by ConnectTimeoutError(<urllib3.connection" +
            ".HTTPConnection object at 0x7f67a5eb0f50>, 'Connection to geobase.qloud.yandex.ru timed out. (connect " +
            "timeout=0.25)'))\\n  File \\\"admin_backend/board/controllers/geo_info.py\\\", line 80, in " +
            "area_center_city_geo_id\\n    parent_geo_ids = ClientsDI.geo_base_client().get_parents_geo_id(geo_id)\\n" +
            "  File \\\"admin_backend/utils/clients/geo.py\\\", line 194, in get_parents_geo_id\\n    response = self" +
            "._get_geobase_resp(method='parents', params={'id': geo_id})\\n  File \\\"talents_common/retry.py\\\", " +
            "line 30, in decorated\\n    return func(*args, **kwargs)\\n  File \\\"admin_backend/utils/clients/geo" +
            ".py\\\", line 221, in _get_geobase_resp\\n    resp = self.session.get(_url, params=params or {}, " +
            "timeout=0.25)  # 250ms\\n  File \\\"requests/sessions.py\\\", line 546, in get\\n    return self.request" +
            "('GET', url, **kwargs)\\n  File \\\"requests/sessions.py\\\", line 533, in request\\n    resp = self" +
            ".send(prep, **send_kwargs)\\n  File \\\"raven/breadcrumbs.py\\\", line 341, in send\\n    resp = " +
            "real_send(self, request, *args, **kwargs)\\n  File \\\"requests/sessions.py\\\", line 646, in send\\n   " +
            " r = adapter.send(request, **kwargs)\\n  File \\\"requests/adapters.py\\\", line 504, in send\\n    " +
            "raise ConnectTimeout(e, request=request)\",\"level\":\"error\",\"reqid\":\"1571759797.76789.122049" +
            ".2619\",\"project\":\"talents\",\"service\":\"test\",\"env\":\"production\",\"language\":\"python\"," +
            "\"timestamp\":1594039195245,\"additional\":{\"test1\":\"qwe\",\"test2\":\"2345678\"," +
            "\"ensure\":\"False\"}}";

        String stacktrace = "timeout: timed out\n" +
            "  File \"urllib3/connection.py\", line 160, in _new_conn\n" +
            "    (self._dns_host, self.port), self.timeout, **extra_kw)\n" +
            "  File \"urllib3/util/connection.py\", line 80, in create_connection\n" +
            "    raise err\n" +
            "  File \"urllib3/util/connection.py\", line 70, in create_connection\n" +
            "    sock.connect(sa)\n" +
            "ConnectTimeoutError: (<urllib3.connection.HTTPConnection object at 0x7f67a5eb0f50>, 'Connection to " +
            "geobase.qloud.yandex.ru timed out. (connect timeout=0.25)')\n" +
            "  File \"urllib3/connectionpool.py\", line 603, in urlopen\n" +
            "    chunked=chunked)\n" +
            "  File \"urllib3/connectionpool.py\", line 355, in _make_request\n" +
            "    conn.request(method, url, **httplib_request_kw)\n" +
            "  File \"http/client.py\", line 1252, in request\n" +
            "    self._send_request(method, url, body, headers, encode_chunked)\n" +
            "  File \"http/client.py\", line 1298, in _send_request\n" +
            "    self.endheaders(body, encode_chunked=encode_chunked)\n" +
            "  File \"http/client.py\", line 1247, in endheaders\n" +
            "    self._send_output(message_body, encode_chunked=encode_chunked)\n" +
            "  File \"http/client.py\", line 1026, in _send_output\n" +
            "    self.send(msg)\n" +
            "  File \"http/client.py\", line 966, in send\n" +
            "    self.connect()\n" +
            "  File \"urllib3/connection.py\", line 183, in connect\n" +
            "    conn = self._new_conn()\n" +
            "  File \"urllib3/connection.py\", line 165, in _new_conn\n" +
            "    (self.host, self.timeout))\n" +
            "MaxRetryError: HTTPConnectionPool(host='geobase.qloud.yandex.ru', port=80): Max retries exceeded with " +
            "url: /v1/parents?id=65 (Caused by ConnectTimeoutError(<urllib3.connection.HTTPConnection object at " +
            "0x7f67a5eb0f50>, 'Connection to geobase.qloud.yandex.ru timed out. (connect timeout=0.25)'))\n" +
            "  File \"requests/adapters.py\", line 449, in send\n" +
            "    timeout=timeout\n" +
            "  File \"urllib3/connectionpool.py\", line 641, in urlopen\n" +
            "    _stacktrace=sys.exc_info()[2])\n" +
            "  File \"urllib3/util/retry.py\", line 399, in increment\n" +
            "    raise MaxRetryError(_pool, url, error or ResponseError(cause))\n" +
            "ConnectTimeout: HTTPConnectionPool(host='geobase.qloud.yandex.ru', port=80): Max retries exceeded with " +
            "url: /v1/parents?id=65 (Caused by ConnectTimeoutError(<urllib3.connection.HTTPConnection object at " +
            "0x7f67a5eb0f50>, 'Connection to geobase.qloud.yandex.ru timed out. (connect timeout=0.25)'))\n" +
            "  File \"admin_backend/board/controllers/geo_info.py\", line 80, in area_center_city_geo_id\n" +
            "    parent_geo_ids = ClientsDI.geo_base_client().get_parents_geo_id(geo_id)\n" +
            "  File \"admin_backend/utils/clients/geo.py\", line 194, in get_parents_geo_id\n" +
            "    response = self._get_geobase_resp(method='parents', params={'id': geo_id})\n" +
            "  File \"talents_common/retry.py\", line 30, in decorated\n" +
            "    return func(*args, **kwargs)\n" +
            "  File \"admin_backend/utils/clients/geo.py\", line 221, in _get_geobase_resp\n" +
            "    resp = self.session.get(_url, params=params or {}, timeout=0.25)  # 250ms\n" +
            "  File \"requests/sessions.py\", line 546, in get\n" +
            "    return self.request('GET', url, **kwargs)\n" +
            "  File \"requests/sessions.py\", line 533, in request\n" +
            "    resp = self.send(prep, **send_kwargs)\n" +
            "  File \"raven/breadcrumbs.py\", line 341, in send\n" +
            "    resp = real_send(self, request, *args, **kwargs)\n" +
            "  File \"requests/sessions.py\", line 646, in send\n" +
            "    r = adapter.send(request, **kwargs)\n" +
            "  File \"requests/adapters.py\", line 504, in send\n" +
            "    raise ConnectTimeout(e, request=request)";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1594039195000L),
            "talents", // PROJECT
            "test", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "1571759797.76789.122049.2619", // REQUEST_ID
            UnsignedLong.valueOf("6340909726585862471"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList("test1", "test2", "ensure"), // KV_KEYS
            Arrays.asList("qwe", "2345678", "False"), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            "Empty error", // MESSAGE
            UnsignedLong.valueOf("1376810649030035901"), // MESSAGE_ID
            "Empty error", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.PYTHON, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("4211541407642377518"), // STACK_TRACE_ID
            stacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("area_center_city_geo_id", "get_parents_geo_id", "decorated", "_get_geobase_resp", "get",
                "request", "send", "send", "send"), // STACK_TRACE_NAMES
            Arrays.asList("admin_backend/board/controllers/geo_info.py", "admin_backend/utils/clients/geo.py",
                "talents_common/retry.py", "admin_backend/utils/clients/geo.py", "requests/sessions.py", "requests" +
                    "/sessions.py", "raven/breadcrumbs.py", "requests/sessions.py", "requests/adapters.py"), //
            // STACK_TRACE_URLS
            Arrays.asList(80, 194, 30, 221, 546, 533, 341, 646, 504), // STACK_TRACE_LINES
            Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.UNIVERSAL, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void javaFieldsParser() throws Exception {
        String line = "{\"project\":\"direct\",\"message\":\"ParameterizedMessage[messagePattern=can't get screenshot" +
            " from Rotor ({}), stringArgs=[[ru.yandex.direct.asynchttp.ErrorResponseWrapperException: Error during " +
            "request, ru.yandex.direct.asynchttp.ErrorResponseWrapperException: Error during request]], " +
            "throwable=null]\",\"timestamp\":1600254022750,\"reqid\":\"813222706212017602\",\"language\":\"java\"," +
            "\"service\":\"direct.canvas\",\"stack\":\"ParameterizedMessage[messagePattern=can't get screenshot from " +
            "Rotor ({}), stringArgs=[[ru.yandex.direct.asynchttp.ErrorResponseWrapperException: Error during request," +
            " ru.yandex.direct.asynchttp.ErrorResponseWrapperException: Error during request]], " +
            "throwable=null]\\n\\tat ru.yandex.canvas.service.RotorService.executeRequest(RotorService.java:85)" +
            "\\n\\tat ru.yandex.canvas.service.RotorService.getScreenshotFromUrl(RotorService.java:64)\\n\\tat ru" +
            ".yandex.canvas.service.ScreenshooterService.getScreenshotFromUrl(ScreenshooterService.java:57)\\n\\tat " +
            "ru.yandex.canvas.service.html5.Html5SourcesService.takeScreenshotFromScreenshooter(Html5SourcesService" +
            ".java:268)\\n\\tat ru.yandex.canvas.service.html5.Html5SourcesService.uploadZip(Html5SourcesService" +
            ".java:383)\\n\\tat ru.yandex.canvas.service.html5.Html5SourcesService.uploadImage(Html5SourcesService" +
            ".java:245)\\n\\tat ru.yandex.canvas.service.html5.Html5SourcesService.uploadSource(Html5SourcesService" +
            ".java:111)\\n\\tat ru.yandex.canvas.controllers.html5.SourceController.uploadSource(SourceController" +
            ".java:41)\\n\\tat jdk.internal.reflect.GeneratedMethodAccessor810.invoke(Unknown Source)\\n\\tat java" +
            ".base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)" +
            "\\n\\tat java.base/java.lang.reflect.Method.invoke(Method.java:566)\\n\\tat org.springframework.web" +
            ".method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:189)\\n\\tat org" +
            ".springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod" +
            ".java:138)\\n\\tat org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod" +
            ".invokeAndHandle(ServletInvocableHandlerMethod.java:102)\\n\\tat org.springframework.web.servlet.mvc" +
            ".method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter" +
            ".java:895)\\n\\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter" +
            ".handleInternal(RequestMappingHandlerAdapter.java:800)\\n\\tat org.springframework.web.servlet.mvc" +
            ".method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87)\\n\\tat org" +
            ".springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1038)\\n\\tat org" +
            ".springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:942)\\n\\tat org" +
            ".springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1005)\\n\\tat org" +
            ".springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:908)\\n\\tat javax.servlet" +
            ".http.HttpServlet.service(HttpServlet.java:707)\\n\\tat org.springframework.web.servlet.FrameworkServlet" +
            ".service(FrameworkServlet.java:882)\\n\\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:790)" +
            "\\n\\tat org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:865)\\n\\tat org.eclipse" +
            ".jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1655)\\n\\tat org.eclipse.jetty" +
            ".websocket.server.WebSocketUpgradeFilter.doFilter(WebSocketUpgradeFilter.java:215)\\n\\tat org.eclipse" +
            ".jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\\n\\tat org.springframework" +
            ".web.filter.AbstractRequestLoggingFilter.doFilterInternal(AbstractRequestLoggingFilter.java:262)\\n\\tat" +
            " org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\\n\\tat org" +
            ".eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\\n\\tat ru.yandex" +
            ".canvas.configs.XCanvasRequestIdHeaderAppenderFilter.doFilterInternal" +
            "(XCanvasRequestIdHeaderAppenderFilter.java:21)\\n\\tat org.springframework.web.filter" +
            ".OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\\n\\tat org.eclipse.jetty.servlet" +
            ".ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\\n\\tat ru.yandex.direct.common.metrics" +
            ".MetricsFilter.doFilterInternal(MetricsFilter.java:62)\\n\\tat org.springframework.web.filter" +
            ".OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\\n\\tat org.eclipse.jetty.servlet" +
            ".ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\\n\\tat ru.yandex.direct.common.tracing" +
            ".TraceContextFilter.doFilterInternal(TraceContextFilter.java:64)\\n\\tat org.springframework.web.filter" +
            ".OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\\n\\tat org.eclipse.jetty.servlet" +
            ".ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\\n\\tat org.springframework.web.filter" +
            ".RequestContextFilter.doFilterInternal(RequestContextFilter.java:99)\\n\\tat org.springframework.web" +
            ".filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\\n\\tat org.eclipse.jetty.servlet" +
            ".ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\\n\\tat org.springframework.web.filter" +
            ".FormContentFilter.doFilterInternal(FormContentFilter.java:92)\\n\\tat org.springframework.web.filter" +
            ".OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\\n\\tat org.eclipse.jetty.servlet" +
            ".ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\\n\\tat org.springframework.web.filter" +
            ".HiddenHttpMethodFilter.doFilterInternal(HiddenHttpMethodFilter.java:93)\\n\\tat org.springframework.web" +
            ".filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\\n\\tat org.eclipse.jetty.servlet" +
            ".ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\\n\\tat org.springframework.web.filter" +
            ".CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:200)\\n\\tat org.springframework" +
            ".web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\\n\\tat org.eclipse.jetty" +
            ".servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\\n\\tat org.eclipse.jetty.servlet" +
            ".ServletHandler.doHandle(ServletHandler.java:533)\\n\\tat org.eclipse.jetty.server.handler.ScopedHandler" +
            ".handle(ScopedHandler.java:146)\\n\\tat org.eclipse.jetty.security.SecurityHandler.handle" +
            "(SecurityHandler.java:548)\\n\\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper" +
            ".java:132)\\n\\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:257)" +
            "\\n\\tat org.eclipse.jetty.server.session.SessionHandler.doHandle(SessionHandler.java:1595)\\n\\tat org" +
            ".eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:255)\\n\\tat org.eclipse.jetty" +
            ".server.handler.ContextHandler.doHandle(ContextHandler.java:1317)\\n\\tat org.eclipse.jetty.server" +
            ".handler.ScopedHandler.nextScope(ScopedHandler.java:203)\\n\\tat org.eclipse.jetty.servlet" +
            ".ServletHandler.doScope(ServletHandler.java:473)\\n\\tat org.eclipse.jetty.server.session.SessionHandler" +
            ".doScope(SessionHandler.java:1564)\\n\\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope" +
            "(ScopedHandler.java:201)\\n\\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler" +
            ".java:1219)\\n\\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:144)" +
            "\\n\\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:132)\\n\\tat org" +
            ".eclipse.jetty.server.Server.handle(Server.java:531)\\n\\tat org.eclipse.jetty.server.HttpChannel.handle" +
            "(HttpChannel.java:352)\\n\\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection" +
            ".java:260)\\n\\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection" +
            ".java:281)\\n\\tat org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:102)\\n\\tat org.eclipse" +
            ".jetty.io.ChannelEndPoint$2.run(ChannelEndPoint.java:118)\\n\\tat org.eclipse.jetty.util.thread.strategy" +
            ".EatWhatYouKill.runTask(EatWhatYouKill.java:333)\\n\\tat org.eclipse.jetty.util.thread.strategy" +
            ".EatWhatYouKill.doProduce(EatWhatYouKill.java:310)\\n\\tat org.eclipse.jetty.util.thread.strategy" +
            ".EatWhatYouKill.tryProduce(EatWhatYouKill.java:168)\\n\\tat org.eclipse.jetty.util.thread.strategy" +
            ".EatWhatYouKill.run(EatWhatYouKill.java:126)\\n\\tat org.eclipse.jetty.util.thread" +
            ".ReservedThreadExecutor$ReservedThread.run(ReservedThreadExecutor.java:366)\\n\\tat org.eclipse.jetty" +
            ".util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:762)\\n\\tat org.eclipse.jetty.util.thread" +
            ".QueuedThreadPool$2.run(QueuedThreadPool.java:680)\\n\\tat java.base/java.lang.Thread.run(Thread" +
            ".java:834)\",\"method\":\"html5.source\",\"host\":\"direct-canvas-sas-yp-3.sas.yp-c.yandex.net\"," +
            "\"env\":\"production\"}";

        String stacktrace = "ParameterizedMessage[messagePattern=can't get screenshot from Rotor ({}), " +
            "stringArgs=[[ru.yandex.direct.asynchttp.ErrorResponseWrapperException: Error during request, ru.yandex" +
            ".direct.asynchttp.ErrorResponseWrapperException: Error during request]], throwable=null]\n" +
            "\tat ru.yandex.canvas.service.RotorService.executeRequest(RotorService.java:85)\n" +
            "\tat ru.yandex.canvas.service.RotorService.getScreenshotFromUrl(RotorService.java:64)\n" +
            "\tat ru.yandex.canvas.service.ScreenshooterService.getScreenshotFromUrl(ScreenshooterService.java:57)\n" +
            "\tat ru.yandex.canvas.service.html5.Html5SourcesService.takeScreenshotFromScreenshooter" +
            "(Html5SourcesService.java:268)\n" +
            "\tat ru.yandex.canvas.service.html5.Html5SourcesService.uploadZip(Html5SourcesService.java:383)\n" +
            "\tat ru.yandex.canvas.service.html5.Html5SourcesService.uploadImage(Html5SourcesService.java:245)\n" +
            "\tat ru.yandex.canvas.service.html5.Html5SourcesService.uploadSource(Html5SourcesService.java:111)\n" +
            "\tat ru.yandex.canvas.controllers.html5.SourceController.uploadSource(SourceController.java:41)\n" +
            "\tat jdk.internal.reflect.GeneratedMethodAccessor810.invoke(Unknown Source)\n" +
            "\tat java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl" +
            ".java:43)\n" +
            "\tat java.base/java.lang.reflect.Method.invoke(Method.java:566)\n" +
            "\tat org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod" +
            ".java:189)\n" +
            "\tat org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest" +
            "(InvocableHandlerMethod.java:138)\n" +
            "\tat org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle" +
            "(ServletInvocableHandlerMethod.java:102)\n" +
            "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter" +
            ".invokeHandlerMethod(RequestMappingHandlerAdapter.java:895)\n" +
            "\tat org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal" +
            "(RequestMappingHandlerAdapter.java:800)\n" +
            "\tat org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle" +
            "(AbstractHandlerMethodAdapter.java:87)\n" +
            "\tat org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1038)\n" +
            "\tat org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:942)\n" +
            "\tat org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1005)\n" +
            "\tat org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:908)\n" +
            "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:707)\n" +
            "\tat org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:882)\n" +
            "\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:790)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:865)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1655)\n" +
            "\tat org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter.doFilter(WebSocketUpgradeFilter.java:215)" +
            "\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\n" +
            "\tat org.springframework.web.filter.AbstractRequestLoggingFilter.doFilterInternal" +
            "(AbstractRequestLoggingFilter.java:262)\n" +
            "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\n" +
            "\tat ru.yandex.canvas.configs.XCanvasRequestIdHeaderAppenderFilter.doFilterInternal" +
            "(XCanvasRequestIdHeaderAppenderFilter.java:21)\n" +
            "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\n" +
            "\tat ru.yandex.direct.common.metrics.MetricsFilter.doFilterInternal(MetricsFilter.java:62)\n" +
            "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\n" +
            "\tat ru.yandex.direct.common.tracing.TraceContextFilter.doFilterInternal(TraceContextFilter.java:64)\n" +
            "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\n" +
            "\tat org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:99)" +
            "\n" +
            "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\n" +
            "\tat org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:92)\n" +
            "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\n" +
            "\tat org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal(HiddenHttpMethodFilter" +
            ".java:93)\n" +
            "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\n" +
            "\tat org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter" +
            ".java:200)\n" +
            "\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1642)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:533)\n" +
            "\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:146)\n" +
            "\tat org.eclipse.jetty.security.SecurityHandler.handle(SecurityHandler.java:548)\n" +
            "\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:132)\n" +
            "\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:257)\n" +
            "\tat org.eclipse.jetty.server.session.SessionHandler.doHandle(SessionHandler.java:1595)\n" +
            "\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:255)\n" +
            "\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1317)\n" +
            "\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:203)\n" +
            "\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:473)\n" +
            "\tat org.eclipse.jetty.server.session.SessionHandler.doScope(SessionHandler.java:1564)\n" +
            "\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:201)\n" +
            "\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1219)\n" +
            "\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:144)\n" +
            "\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:132)\n" +
            "\tat org.eclipse.jetty.server.Server.handle(Server.java:531)\n" +
            "\tat org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:352)\n" +
            "\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:260)\n" +
            "\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:281)\n" +
            "\tat org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:102)\n" +
            "\tat org.eclipse.jetty.io.ChannelEndPoint$2.run(ChannelEndPoint.java:118)\n" +
            "\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.runTask(EatWhatYouKill.java:333)\n" +
            "\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.doProduce(EatWhatYouKill.java:310)\n" +
            "\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.tryProduce(EatWhatYouKill.java:168)\n" +
            "\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.run(EatWhatYouKill.java:126)\n" +
            "\tat org.eclipse.jetty.util.thread.ReservedThreadExecutor$ReservedThread.run(ReservedThreadExecutor" +
            ".java:366)\n" +
            "\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:762)\n" +
            "\tat org.eclipse.jetty.util.thread.QueuedThreadPool$2.run(QueuedThreadPool.java:680)\n" +
            "\tat java.base/java.lang.Thread.run(Thread.java:834)";

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1600254022000L),
            "direct", // PROJECT
            "direct.canvas", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "813222706212017602", // REQUEST_ID
            UnsignedLong.valueOf("4489402391051254339"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList(), // KV_KEYS
            Arrays.asList(), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            "ParameterizedMessage[messagePattern=can't get screenshot from Rotor ({}), stringArgs=[[ru.yandex.direct" +
                ".asynchttp.ErrorResponseWrapperException: Error during request, ru.yandex.direct.asynchttp" +
                ".ErrorResponseWrapperException: Error during request]], throwable=null]", // MESSAGE
            UnsignedLong.valueOf("1026011360468617607"), // MESSAGE_ID
            "ParameterizedMessage[messagePattern=can't get screenshot from Rotor ({}), stringArgs=[[ru.yandex.direct" +
                ".asynchttp.ErrorResponseWrapperException: Error during request, ru.yandex.direct.asynchttp" +
                ".ErrorResponseWrapperException: Error during request]], throwable=null]", // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.JAVA, // RUNTIME
            LogLevel.UNKNOWN, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "html5.source", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("8225497744105799151"), // STACK_TRACE_ID
            stacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("ru.yandex.canvas.service.RotorService.executeRequest", "ru.yandex.canvas.service" +
                ".RotorService.getScreenshotFromUrl", "ru.yandex.canvas.service.ScreenshooterService" +
                ".getScreenshotFromUrl", "ru.yandex.canvas.service.html5.Html5SourcesService" +
                ".takeScreenshotFromScreenshooter", "ru.yandex.canvas.service.html5.Html5SourcesService.uploadZip",
                "ru.yandex.canvas.service.html5.Html5SourcesService.uploadImage", "ru.yandex.canvas.service.html5" +
                    ".Html5SourcesService.uploadSource", "ru.yandex.canvas.controllers.html5.SourceController" +
                    ".uploadSource", "jdk.internal.reflect.GeneratedMethodAccessor810.invoke", "java.base/jdk" +
                    ".internal.reflect.DelegatingMethodAccessorImpl.invoke", "java.base/java.lang.reflect.Method" +
                    ".invoke", "org.springframework.web.method.support.InvocableHandlerMethod.doInvoke", "org" +
                    ".springframework.web.method.support.InvocableHandlerMethod.invokeForRequest", "org" +
                    ".springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod" +
                    ".invokeAndHandle", "org.springframework.web.servlet.mvc.method.annotation" +
                    ".RequestMappingHandlerAdapter.invokeHandlerMethod", "org.springframework.web.servlet.mvc.method" +
                    ".annotation.RequestMappingHandlerAdapter.handleInternal", "org.springframework.web.servlet.mvc" +
                    ".method.AbstractHandlerMethodAdapter.handle", "org.springframework.web.servlet.DispatcherServlet" +
                    ".doDispatch", "org.springframework.web.servlet.DispatcherServlet.doService", "org" +
                    ".springframework.web.servlet.FrameworkServlet.processRequest", "org.springframework.web.servlet" +
                    ".FrameworkServlet.doPost", "javax.servlet.http.HttpServlet.service", "org.springframework.web" +
                    ".servlet.FrameworkServlet.service", "javax.servlet.http.HttpServlet.service", "org.eclipse.jetty" +
                    ".servlet.ServletHolder.handle", "org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter",
                "org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter.doFilter", "org.eclipse.jetty.servlet" +
                    ".ServletHandler$CachedChain.doFilter", "org.springframework.web.filter" +
                    ".AbstractRequestLoggingFilter.doFilterInternal", "org.springframework.web.filter" +
                    ".OncePerRequestFilter.doFilter",
                "org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter",
                "ru.yandex.canvas.configs.XCanvasRequestIdHeaderAppenderFilter.doFilterInternal", "org" +
                    ".springframework.web.filter.OncePerRequestFilter.doFilter", "org.eclipse.jetty.servlet" +
                    ".ServletHandler$CachedChain.doFilter", "ru.yandex.direct.common.metrics.MetricsFilter" +
                    ".doFilterInternal", "org.springframework.web.filter.OncePerRequestFilter.doFilter", "org.eclipse" +
                    ".jetty.servlet.ServletHandler$CachedChain.doFilter", "ru.yandex.direct.common.tracing" +
                    ".TraceContextFilter.doFilterInternal", "org.springframework.web.filter.OncePerRequestFilter" +
                    ".doFilter", "org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter", "org" +
                    ".springframework.web.filter.RequestContextFilter.doFilterInternal", "org.springframework.web" +
                    ".filter.OncePerRequestFilter.doFilter", "org.eclipse.jetty.servlet.ServletHandler$CachedChain" +
                    ".doFilter", "org.springframework.web.filter.FormContentFilter.doFilterInternal", "org" +
                    ".springframework.web.filter.OncePerRequestFilter.doFilter", "org.eclipse.jetty.servlet" +
                    ".ServletHandler$CachedChain.doFilter", "org.springframework.web.filter.HiddenHttpMethodFilter" +
                    ".doFilterInternal", "org.springframework.web.filter.OncePerRequestFilter.doFilter", "org.eclipse" +
                    ".jetty.servlet.ServletHandler$CachedChain.doFilter", "org.springframework.web.filter" +
                    ".CharacterEncodingFilter.doFilterInternal", "org.springframework.web.filter.OncePerRequestFilter" +
                    ".doFilter", "org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter", "org.eclipse.jetty" +
                    ".servlet.ServletHandler.doHandle", "org.eclipse.jetty.server.handler.ScopedHandler.handle", "org" +
                    ".eclipse.jetty.security.SecurityHandler.handle", "org.eclipse.jetty.server.handler" +
                    ".HandlerWrapper.handle", "org.eclipse.jetty.server.handler.ScopedHandler.nextHandle", "org" +
                    ".eclipse.jetty.server.session.SessionHandler.doHandle", "org.eclipse.jetty.server.handler" +
                    ".ScopedHandler.nextHandle", "org.eclipse.jetty.server.handler.ContextHandler.doHandle", "org" +
                    ".eclipse.jetty.server.handler.ScopedHandler.nextScope", "org.eclipse.jetty.servlet" +
                    ".ServletHandler.doScope", "org.eclipse.jetty.server.session.SessionHandler.doScope", "org" +
                    ".eclipse.jetty.server.handler.ScopedHandler.nextScope", "org.eclipse.jetty.server.handler" +
                    ".ContextHandler.doScope", "org.eclipse.jetty.server.handler.ScopedHandler.handle", "org.eclipse" +
                    ".jetty.server.handler.HandlerWrapper.handle", "org.eclipse.jetty.server.Server.handle", "org" +
                    ".eclipse.jetty.server.HttpChannel.handle", "org.eclipse.jetty.server.HttpConnection.onFillable",
                "org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded", "org.eclipse.jetty.io.FillInterest" +
                    ".fillable", "org.eclipse.jetty.io.ChannelEndPoint$2.run", "org.eclipse.jetty.util.thread" +
                    ".strategy.EatWhatYouKill.runTask", "org.eclipse.jetty.util.thread.strategy.EatWhatYouKill" +
                    ".doProduce", "org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.tryProduce", "org.eclipse" +
                    ".jetty.util.thread.strategy.EatWhatYouKill.run", "org.eclipse.jetty.util.thread" +
                    ".ReservedThreadExecutor$ReservedThread.run", "org.eclipse.jetty.util.thread.QueuedThreadPool" +
                    ".runJob", "org.eclipse.jetty.util.thread.QueuedThreadPool$2.run", "java.base/java.lang.Thread" +
                    ".run"), // STACK_TRACE_NAMES
            Arrays.asList("RotorService.java", "RotorService.java", "ScreenshooterService.java", "Html5SourcesService" +
                ".java", "Html5SourcesService.java", "Html5SourcesService.java", "Html5SourcesService.java",
                "SourceController.java", "Unknown Source", "DelegatingMethodAccessorImpl.java", "Method.java",
                "InvocableHandlerMethod.java", "InvocableHandlerMethod.java", "ServletInvocableHandlerMethod.java",
                "RequestMappingHandlerAdapter.java", "RequestMappingHandlerAdapter.java",
                "AbstractHandlerMethodAdapter.java", "DispatcherServlet.java", "DispatcherServlet.java",
                "FrameworkServlet.java", "FrameworkServlet.java", "HttpServlet.java", "FrameworkServlet.java",
                "HttpServlet.java", "ServletHolder.java", "ServletHandler.java", "WebSocketUpgradeFilter.java",
                "ServletHandler.java", "AbstractRequestLoggingFilter.java", "OncePerRequestFilter.java",
                "ServletHandler.java", "XCanvasRequestIdHeaderAppenderFilter.java", "OncePerRequestFilter.java",
                "ServletHandler.java", "MetricsFilter.java", "OncePerRequestFilter.java", "ServletHandler.java",
                "TraceContextFilter.java", "OncePerRequestFilter.java", "ServletHandler.java", "RequestContextFilter" +
                    ".java", "OncePerRequestFilter.java", "ServletHandler.java", "FormContentFilter.java",
                "OncePerRequestFilter.java", "ServletHandler.java", "HiddenHttpMethodFilter.java",
                "OncePerRequestFilter.java", "ServletHandler.java", "CharacterEncodingFilter.java",
                "OncePerRequestFilter.java", "ServletHandler.java", "ServletHandler.java", "ScopedHandler.java",
                "SecurityHandler.java", "HandlerWrapper.java", "ScopedHandler.java", "SessionHandler.java",
                "ScopedHandler.java", "ContextHandler.java", "ScopedHandler.java", "ServletHandler.java",
                "SessionHandler.java", "ScopedHandler.java", "ContextHandler.java", "ScopedHandler.java",
                "HandlerWrapper.java", "Server.java", "HttpChannel.java", "HttpConnection.java", "AbstractConnection" +
                    ".java", "FillInterest.java", "ChannelEndPoint.java", "EatWhatYouKill.java", "EatWhatYouKill" +
                    ".java", "EatWhatYouKill.java", "EatWhatYouKill.java", "ReservedThreadExecutor.java",
                "QueuedThreadPool.java", "QueuedThreadPool.java", "Thread.java"), // STACK_TRACE_URLS
            Arrays.asList(85, 64, 57, 268, 383, 245, 111, 41, 0, 43, 566, 189, 138, 102, 895, 800, 87, 1038,
                942, 1005, 908, 707, 882, 790, 865, 1655, 215, 1642, 262, 107, 1642, 21, 107, 1642, 62, 107, 1642,
                64, 107, 1642, 99, 107, 1642, 92, 107, 1642, 93, 107, 1642, 200, 107, 1642, 533, 146, 548, 132,
                257, 1595, 255, 1317, 203, 473, 1564, 201, 1219, 144, 132, 531, 352, 260, 281, 102, 118, 333, 310,
                168, 126, 366, 762, 680, 834), // STACK_TRACE_LINES
            Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.UNIVERSAL, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            "direct-canvas-sas-yp-3.sas.yp-c.yandex.net", // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void skipWithoutProject() throws Exception {
        String line = "{\"message\":\"Column not found: 1\",\"timestamp\":1571747133800}";

        checker.checkEmpty(line);
    }

    @Test
    public void skipWithoutTimestamp() throws Exception {
        String line = "{\"message\":\"Column not found: 1\",\"project\":\"error-booster\"}";

        checker.checkEmpty(line);
    }

    @Test
    public void skipWithSkipRules() throws Exception {
        String line = "{\"message\":\"Column not found: 1\",\"project\":\"error-booster\",\"timestamp\":1571747133800}";
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": []}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"message\", \"operator\": \"startsWith\", " +
            "\"val1ue\": \"Column not\"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"message\", \"ope1rator\": \"startsWith\", " +
            "\"value\": \"Column not\"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"fi1eld\":\"message\", \"operator\": \"startsWith\", " +
            "\"value\": \"Column not\"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"message\", \"operator\": \"start1sWith\", " +
            "\"value\": \"Column not\"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_0\": [{\"field\":\"me1ssage\", \"operator\": \"startsWith\", " +
            "\"value\": \"Column not\"}], \"case_1\": [{\"field\":\"message\", \"operator\": \"start1sWith\", " +
            "\"value\": \"Column not\"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"me1ssage\", \"operator\": \"startsWith\", " +
            "\"value\": \"Column not\"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_0\": [{\"field\":\"message\", \"operator\": \"startsWith\", \"value\":" +
            " \"bbbbb\"}], \"case_1\": [{\"field\":\"message\", \"operator\": \"startsWith\", \"value\": \"Column " +
            "not\"}]}");
        checker.checkEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"message\", \"operator\": \"startsWith\", \"value\":" +
            " \"Column not\"}]}");
        checker.checkEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"message\", \"operator\": \"endsWith\", \"value\": " +
            "\": 1\"}]}");
        checker.checkEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"message\", \"operator\": \"contains\", \"value\": " +
            "\" not \"}]}");
        checker.checkEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"message\", \"operator\": \"startsWith\", \"value\":" +
            " \"Co1lumn not\"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"message\", \"operator\": \"endsWith\", \"value\": " +
            "\": 12\"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"message\", \"operator\": \"contains\", \"value\": " +
            "\" no2t \"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"project\", \"operator\": \"equals\", \"value\": " +
            "\"error-booster\"}, {\"field\":\"message\", \"operator\": \"startsWith\", \"value\": \"Column not\"}]}");
        checker.checkEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"project\", \"operator\": \"equals\", \"value\": " +
            "\"error-booster\"}, {\"field\":\"message\", \"operator\": \"endsWith\", \"value\": \": 1\"}]}");
        checker.checkEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"project\", \"operator\": \"equals\", \"value\": " +
            "\"error-booster\"}, {\"field\":\"message\", \"operator\": \"endsWith\", \"value\": \": 1\"}]}");
        checker.checkEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"project\", \"operator\": \"equals\", \"value\": " +
            "\"error\"}, {\"field\":\"message\", \"operator\": \"startsWith\", \"value\": \"Column not\"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_1\": [{\"field\":\"project\", \"operator\": \"equals\", \"value\": " +
            "\"error\"}, {\"field\":\"message\", \"operator\": \"endsWith\", \"value\": \": 1\"}]}");
        checker.checkNotEmpty(line);

        checker.setParam("skipRules", "{\"case_0\": [{\"field\":\"project\", \"operator\": \"equals\", \"value\": " +
            "\"zen\"}, {\"field\":\"message\", \"operator\": \"startsWith\", \"value\": \"Feed Images\"}],\"case_1\":" +
            " [{\"field\":\"project\", \"operator\": \"equals\", \"value\": \"error-booster\"}, " +
            "{\"field\":\"message\", \"operator\": \"startsWith\", \"value\": \"Column not found\"}]}");
        checker.checkEmpty(line);
    }
}
