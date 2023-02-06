package ru.yandex.market.crm.triggers.logback;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author zloddey
 */
class TriggersPlatformDataCleaningMessageConverterTest {
    private final TriggersPlatformDataCleaningMessageConverter converter =
            new TriggersPlatformDataCleaningMessageConverter();

    /**
     * Примеры строк, которые <b>не должны</b> перезаписываться в конвертере
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "org.camunda.bpm.engine.delegate.BpmnError: No email address is found",
            "Consumer stream (sessionId: market-lilucrm@test@triggers-platform_185_70489_243765432109843394) closed",
            "Cannot find local datacenter: iva.yt.yandex.net among: [seneca-sas, seneca-man, hahn]"
    })
    public void notTransformedMessages(String messageBefore) {
        String messageAfter = converter.convert(messageBefore);
        assertEquals(messageBefore, messageAfter);
    }

    /**
     * Примеры строк, которые <b>должны</b> перезаписываться в конвертере
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("messageTransformationExamples")
    public void messageTransformations(String name, String originalMessage, String expectedText) {
        String convertedText = converter.convert(originalMessage);
        assertEquals(expectedText, convertedText);
    }

    /**
     * Источник примеров для теста {@link TriggersPlatformDataCleaningMessageConverterTest#messageTransformations}
     */
    private static Stream<Arguments> messageTransformationExamples() {
        return Stream.of(
                Arguments.of("Remove ProcessInstance ID",
                        "Execution of 'UPDATE ProcessInstance[ab1fe706-d082-43ff-83ab-b082ebc30962]' failed.",
                        "Execution of 'UPDATE ProcessInstance' failed."),
                Arguments.of("Remove MessageEntity ID, no nesting",
                        "Execution of 'UPDATE MessageEntity[repeat=null, id=acc8c472-12b0-4348-84ca-2346ef68a7e1, " +
                        "revision=6, duedate=Wed Sep 01 17:45:31 MSK 2021, " +
                        "lockOwner=cb8e0095-c260-48d5-9069-3403924aeef1, " +
                        "lockExpirationTime=Wed Sep 01 17:50:31 MSK 2021, " +
                        "executionId=bdb527bf-e6be-43ab-8c0e-9a1083ee44d3, " +
                        "processInstanceId=8bafb919-6cc3-405d-89fd-db501548015c, isExclusive=false, " +
                        "retries=3, jobHandlerType=async-continuation, " +
                        "jobHandlerConfiguration=transition-notify-listener-take$SequenceFlow_17awcaq, " +
                        "exceptionByteArray=null, exceptionByteArrayId=5d162276-c278-446b-a3ae-60e7869f5263, " +
                        "exceptionMessage=Something bad, deploymentId=38d251b3-ff1b-4716-bed8-4e6d95ccc630]' " +
                        "failed. Entity was updated by another transaction",
                        "Execution of 'UPDATE MessageEntity' failed. Entity was updated by another transaction"),
                Arguments.of("Remove MessageEntity ID, with nesting",
                        "Execution of 'UPDATE MessageEntity[repeat=null, id=acc8c472-12b0-4348-84ca-2346ef68a7e1, " +
                        "revision=6, duedate=Wed Sep 01 17:45:31 MSK 2021, " +
                        "lockOwner=cb8e0095-c260-48d5-9069-3403924aeef1, " +
                        "lockExpirationTime=Wed Sep 01 17:50:31 MSK 2021, " +
                        "executionId=bdb527bf-e6be-43ab-8c0e-9a1083ee44d3, " +
                        "processInstanceId=8bafb919-6cc3-405d-89fd-db501548015c, isExclusive=false, " +
                        "retries=3, jobHandlerType=async-continuation, " +
                        "jobHandlerConfiguration=transition-notify-listener-take$SequenceFlow_17awcaq, " +
                        "exceptionByteArray=null, exceptionByteArrayId=5d162276-c278-446b-a3ae-60e7869f5263, " +
                        "exceptionMessage=ENGINE-03005 Execution of 'DELETE MessageEntity[repeat=null, " +
                        "id=acc8c472-12b0-4348-84ca-2346ef68a7e1, revision=2, duedate=Wed Sep 01 17:45:31 MSK 2021, " +
                        "lockOwner=cb8e0095-c260-48d5-9069-3403924aeef1, " +
                        "lockExpirationTime=Wed Sep 01 17:50:31 MSK 2021, " +
                        "executionId=8bafb919-6cc3-405d-89fd-db501548015c, " +
                        "processInstanceId=8bafb919-6cc3-405d-89fd-db501548015c, isExclusive=false, " +
                        "retries=3, jobHandlerType=async-continuation, " +
                        "jobHandlerConfiguration=transition-notify-listener-take$SequenceFlow_17awcaq, " +
                        "exceptionByteArray=null, exceptionByteArrayId=null, exceptionMessage=null, " +
                        "deploymentId=38d251b3-ff1b-4716-bed8-4e6d95ccc630]' failed. Entity was updated by " +
                        "another tra, deploymentId=38d251b3-ff1b-4716-bed8-4e6d95ccc630, arr=[1,2,3,4,5]]' " +
                        "failed. Entity was updated by another transaction",
                        "Execution of 'UPDATE MessageEntity' failed. Entity was updated by another transaction"),
                Arguments.of("Remove MessageEntity ID, no finishing bracket",
                        "Execution of 'UPDATE MessageEntity[repeat=null, id=acc8c472-12b0-4348-84ca-2346ef68a7e1, " +
                        "revision=6, duedate=Wed Sep 01 17:45:31 MSK 2021, " +
                        "lockOwner=cb8e0095-c260-48d5-9069-3403924aeef1, " +
                        "lockExpirationTime=Wed Sep 01 17:50:31 MSK 2021, " +
                        "executionId=bdb527bf-e6be-43ab-8c0e-9a1083ee44d3, " +
                        "processInstanceId=8bafb919-6cc3-405d-89fd-db501548015c, isExclusive=false, " +
                        "retries=3, jobHandlerType=async-continuation, " +
                        "jobHandlerConfiguration=transition-notify-listener-take$SequenceFlow_17awcaq, " +
                        "exceptionByteArray=null, exceptionByteArrayId=5...",
                        "Execution of 'UPDATE MessageEntity"),
                Arguments.of("Remove phone number",
                        "Couldn't send sms to 77777777777; Caused by: java.util.concurrent.ExecutionException: " +
                        "java.lang.RuntimeException: LIMITEXCEEDED: Sms limit for this phone exceeded " +
                        "(+77777777777)",
                        "Couldn't send sms to %PHONE%; Caused by: java.util.concurrent.ExecutionException: " +
                        "java.lang.RuntimeException: LIMITEXCEEDED: Sms limit for this phone exceeded " +
                        "(%PHONE%)")
        );
    }
}
