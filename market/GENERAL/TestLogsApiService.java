package ru.yandex.market.chef.api;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import ru.yandex.mj.generated.server.api.TestLogsApiDelegate;
import ru.yandex.mj.generated.server.model.TestLogReqDto;
import ru.yandex.mj.generated.server.model.TestLogRespDto;


@Component
public class TestLogsApiService implements TestLogsApiDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(TestLogsApiService.class);

    @Value("${chef.example.credential.key:not defined}")
    private String exampleKey;
    @Value("${chef.example.credential.subkey:not defined}")
    private String exampleSubKey;
    @Value("${chef.props.value.parent: not defined}")
    private String parentValue;
    @Value("${chef.props.value.overrided: not defined}")
    private String overridedValue;

    @Override
    public ResponseEntity<TestLogRespDto> v1TestLogsPost(TestLogReqDto testLogReqDto) {
        LOG.debug("LOG DEBUG");
        LOG.info("LOG INFO");
        LOG.warn("LOG WARN");
        LOG.error("LOG ERROR");
        LOG.info("requestsBody: " + testLogReqDto);
        LOG.info("exampleKey: {}, exampleSubkey: {}", exampleKey, exampleSubKey);
        LOG.info("parent value {}, overrided value: {}", parentValue, overridedValue);
        return ResponseEntity.ok(new TestLogRespDto()
                .field(testLogReqDto.getName())
                .value(testLogReqDto.getNum())
                .timestamp(OffsetDateTime.now())
        );
    }
}
