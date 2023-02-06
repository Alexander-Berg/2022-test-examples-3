package ru.yandex.autotests.innerpochta.imap.core.imap;

import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import ru.yandex.autotests.innerpochta.imap.requests.ImapRequest;
import ru.yandex.autotests.innerpochta.imap.requests.ImapRequestBuilder;
import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;
import ru.yandex.autotests.innerpochta.imap.rules.TestWatcherWithExceptions;
import ru.yandex.autotests.innerpochta.imap.steps.AppendSteps;
import ru.yandex.autotests.innerpochta.imap.steps.CopySteps;
import ru.yandex.autotests.innerpochta.imap.steps.ExamineSteps;
import ru.yandex.autotests.innerpochta.imap.steps.FetchSteps;
import ru.yandex.autotests.innerpochta.imap.steps.ListSteps;
import ru.yandex.autotests.innerpochta.imap.steps.LsubSteps;
import ru.yandex.autotests.innerpochta.imap.steps.NoopSteps;
import ru.yandex.autotests.innerpochta.imap.steps.SearchSteps;
import ru.yandex.autotests.innerpochta.imap.steps.SelectSteps;
import ru.yandex.autotests.innerpochta.imap.steps.StatusSteps;
import ru.yandex.autotests.innerpochta.imap.steps.StoreSteps;
import ru.yandex.junitextensions.rules.loginrule.Credentials;
import ru.yandex.qatools.allure.annotations.Attachment;
import ru.yandex.qatools.allure.annotations.Step;

import static com.google.common.base.Joiner.on;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.ConnectionTypes.DEFAULT;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.ConnectionTypes.SSL;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;
import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;

public final class ImapClient implements TestRule {

    private ImapRequest lastRequest;
    private ImapResponse lastResponse;

    private boolean asynchronous = false;
    private ImapSessionImpl session;
    private int commandCounter = 0;
    private List<TestRule> rules = new LinkedList<>();

    public ImapClient() {
        session = new ImapSessionImpl(props().getHost(), props().getPort(), props().getConnectionType());
        rules.add(session);
    }

    public ImapClient onPlainPort() {
        rules.remove(session);
        session = new ImapSessionImpl(props().getHost(), props().getPlainPort(), DEFAULT.value());
        rules.add(session);
        return this;
    }

    public ImapClient onSslPort() {
        rules.remove(session);
        session = new ImapSessionImpl(props().getHost(), props().getSslPort(), SSL.value());
        rules.add(session);
        return this;
    }

    /**
     * Метод написан специально для теста STARTTLS
     *
     * @return
     */
    public ImapClient startEncrypt() throws Throwable {
        session.addSslFilter(session.getSession());
        return this;
    }

    /**
     * Создаем новую сессию.
     * <p>
     * Метод написан специально для теста KillSessionTest (MPROTO-1652)
     *
     * @return
     */
    public ImapClient newSession() throws Throwable {
        session = new ImapSessionImpl(props().getHost(), props().getPort(), props().getConnectionType());
        session.before();
        rules.add(session);
        return this;
    }

    /**
     * Прибиваем сессию сессию.
     * <p>
     * Метод написан специально для теста KillSessionTest (MPROTO-1652)
     *
     * @return
     */
    public ImapClient killSession() throws NoSuchAlgorithmException {
        session.close();
        rules.remove(session);
        return this;
    }

    public ImapClient around(TestRule rule) {
        rules.add(0, rule);
        return this;
    }

    public ImapClient wrapBy(TestRule rule) {
        rules.add(rule);
        return this;
    }

    /**
     * Только запись.
     *
     * @return this
     */
    public ImapClient asynchronous() {
        this.asynchronous = true;
        return this;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new RunRules(base, rules, description);
    }


    public <T extends ImapResponse<T>> T request(ImapRequestBuilder<T> requestBuilder) {
        return request(requestBuilder.build(getNextTag()));
    }


    @Step("Запрос [{0}]")
    public <T extends ImapResponse<T>> T request(ImapRequest<T> request) {
        T response = null;

        attach("Client", on("\n").join(request.getLines()));

        for (String line : request.getLines()) {
            if (response != null) {
                response.shouldBeContinuationRequest();
            }
            session.writeLine(line);
            lastRequest = request;

            if (!asynchronous) {
                response = readResponse(request.getResponseClass(), request);
                lastResponse = response;
            }
        }

        if (!request.isComplex() && !asynchronous) {
            response.validateForOkNoBad();
        }
        asynchronous = false;
        return response;
    }

    public ImapSessionImpl activeSession() {
        return session;
    }

    @Deprecated
    @Step
    public void messageShouldBeReceived() {
        this.noop().messageShouldBeReceived();
//        assertThat(this, withWaitFor(somethingHappens(), TIMEOUT, SECONDS));
    }

    public ListSteps list() {
        return ListSteps.with(this);
    }

    public SelectSteps select() {
        return SelectSteps.with(this);
    }

    public ExamineSteps examine() {
        return ExamineSteps.with(this);
    }

    public LsubSteps lsub() {
        return LsubSteps.with(this);
    }

    public AppendSteps append() {
        return AppendSteps.with(this);
    }

    public StatusSteps status() {
        return StatusSteps.with(this);
    }

    public CopySteps copy() {
        return CopySteps.with(this);
    }

    public StoreSteps store() {
        return StoreSteps.with(this);
    }

    public SearchSteps search() {
        return SearchSteps.with(this);
    }

    public FetchSteps fetch() {
        return FetchSteps.with(this);
    }

    public NoopSteps noop() {
        return NoopSteps.with(this);
    }

    public ImapClient loginWith(Credentials acc) {
        return withReqOnStart(login(acc));
    }

    private <T extends ImapResponse<T>> ImapClient withReqOnStart(final ImapRequestBuilder<T> req) {
        rules.add(0, new TestWatcherWithExceptions() {
            @Override
            protected void starting(Description description) throws Exception {
                request(req).shouldBeOk();
            }
        });
        return this;
    }

    //читаем ответ от асинхронного запроса
    public <T extends ImapResponse<T>> T readFuture(Class<T> clazz) {
        return readResponse(clazz, lastRequest);
    }

    private <T extends ImapResponse<T>> T readResponse(Class<T> clazz, ImapRequest<?> request) {
        return readResponseInto(ImapResponse.newResponseInstance(clazz), request);
    }

    private <T extends ImapResponse<T>> T readResponseInto(T response, ImapRequest<?> request) {
        response.setRequest(request);

        if (request.isComplex()) {
            //добавляем первую строчку и на этом успокаиваемся
            response.add(session.readLine());
        } else {
            while (!response.isComplete()) {
                response.add(session.readLine());
            }
        }

        attach("Server", on("\n").join(response.lines()));
        return response;
    }


    private String getNextTag() {
        return String.format("%s.%s.%s", session.id(), session.usertag(), String.valueOf(commandCounter++));
    }

    public ImapRequest getLastRequest() {
        return lastRequest;
    }

    public ImapResponse getLastResponse() {
        return lastResponse;
    }

    @Attachment("{0}-lines")
    private String attach(String who, String what) {
        return what;
    }
}
