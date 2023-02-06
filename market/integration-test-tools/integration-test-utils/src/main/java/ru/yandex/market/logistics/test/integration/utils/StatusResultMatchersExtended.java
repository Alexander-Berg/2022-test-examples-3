package ru.yandex.market.logistics.test.integration.utils;

import lombok.SneakyThrows;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Копипаста спрингового матчера с выводом бади в ошибке.
 *
 * @see org.springframework.test.web.servlet.result.StatusResultMatchers
 */
public class StatusResultMatchersExtended {
    /**
     * Protected constructor.
     * Use {@link IntegrationTestUtils#status()}.
     */
    StatusResultMatchersExtended() {
    }

    /**
     * Assert the response status code is in the 2xx range.
     */
    public ResultMatcher is2xxSuccessful() {
        return matcher(HttpStatus.Series.SUCCESSFUL);
    }

    /**
     * Assert the response status code is in the 4xx range.
     */
    public ResultMatcher is4xxClientError() {
        return matcher(HttpStatus.Series.CLIENT_ERROR);
    }

    /**
     * Assert the response status code is in the 5xx range.
     */
    public ResultMatcher is5xxServerError() {
        return matcher(HttpStatus.Series.SERVER_ERROR);
    }

    /**
     * Assert the Servlet response error message.
     */
    public ResultMatcher reason(final String reason) {
        return result -> assertEquals("Response status reason", reason, result.getResponse().getErrorMessage());
    }

    /**
     * Assert the response status code is {@code HttpStatus.OK} (200).
     */
    public ResultMatcher isOk() {
        return matcher(HttpStatus.OK);
    }

    /**
     * Assert the response status code is {@code HttpStatus.CREATED} (201).
     */
    public ResultMatcher isCreated() {
        return matcher(HttpStatus.CREATED);
    }

    /**
     * Assert the response status code is {@code HttpStatus.ALREADY_REPORTED} (208).
     */
    public ResultMatcher isAlreadyReported() {
        return matcher(HttpStatus.ALREADY_REPORTED);
    }

    /**
     * Assert the response status code is {@code HttpStatus.BAD_REQUEST} (400).
     */
    public ResultMatcher isBadRequest() {
        return matcher(HttpStatus.BAD_REQUEST);
    }

    /**
     * Assert the response status code is {@code HttpStatus.UNAUTHORIZED} (401).
     */
    public ResultMatcher isUnauthorized() {
        return matcher(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Assert the response status code is {@code HttpStatus.PAYMENT_REQUIRED} (402).
     */
    public ResultMatcher isPaymentRequired() {
        return matcher(HttpStatus.PAYMENT_REQUIRED);
    }

    /**
     * Assert the response status code is {@code HttpStatus.FORBIDDEN} (403).
     */
    public ResultMatcher isForbidden() {
        return matcher(HttpStatus.FORBIDDEN);
    }

    /**
     * Assert the response status code is {@code HttpStatus.NOT_FOUND} (404).
     */
    public ResultMatcher isNotFound() {
        return matcher(HttpStatus.NOT_FOUND);
    }

    /**
     * Assert the response status code is {@code HttpStatus.METHOD_NOT_ALLOWED} (405).
     */
    public ResultMatcher isMethodNotAllowed() {
        return matcher(HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Assert the response status code is {@code HttpStatus.NOT_ACCEPTABLE} (406).
     */
    public ResultMatcher isNotAcceptable() {
        return matcher(HttpStatus.NOT_ACCEPTABLE);
    }

    /**
     * Assert the response status code is {@code HttpStatus.CONFLICT} (409).
     */
    public ResultMatcher isConflict() {
        return matcher(HttpStatus.CONFLICT);
    }

    /**
     * Assert the response status code is {@code HttpStatus.PRECONDITION_FAILED} (412).
     */
    public ResultMatcher isPreconditionFailed() {
        return matcher(HttpStatus.PRECONDITION_FAILED);
    }

    /**
     * Assert the response status code is {@code HttpStatus.UNPROCESSABLE_ENTITY} (422).
     */
    public ResultMatcher isUnprocessableEntity() {
        return matcher(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Assert the response status code is {@code HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS} (451).
     *
     * @since 4.3
     */
    public ResultMatcher isUnavailableForLegalReasons() {
        return matcher(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
    }

    /**
     * Assert the response status code is {@code HttpStatus.INTERNAL_SERVER_ERROR} (500).
     */
    public ResultMatcher isInternalServerError() {
        return matcher(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Match the expected response status to that of the HttpServletResponse.
     */
    private ResultMatcher matcher(final HttpStatus status) {
        return result -> setFailMessage(Assertions.assertThat(result.getResponse().getStatus()), status.value(), result)
            .isEqualTo(status.value());
    }

    private ResultMatcher matcher(final HttpStatus.Series statuses) {
        return result -> setFailMessage(
            Assertions.assertThat(
                HttpStatus.Series.resolve(result.getResponse().getStatus()) == statuses),
            statuses,
            result
        ).isEqualTo(true);
    }

    @SneakyThrows
    private AbstractAssert<?, ?> setFailMessage(AbstractAssert<?, ?> assertion, int status, MvcResult result) {
        return assertion.withFailMessage(
            "Status expected:<%s> but was:<%s>. Body: '%s'",
            status,
            result.getResponse().getStatus(),
            result.getResponse().getContentAsString()
        );
    }

    @SneakyThrows
    private AbstractAssert<?, ?> setFailMessage(
        AbstractAssert<?, ?> assertion,
        HttpStatus.Series statuses,
        MvcResult result
    ) {
        return assertion.withFailMessage(
            "Range for response status value expected:<%s> but was:<%s>. Body: '%s'",
            statuses.name(),
            result.getResponse().getStatus(),
            result.getResponse().getContentAsString()
        );
    }
}
