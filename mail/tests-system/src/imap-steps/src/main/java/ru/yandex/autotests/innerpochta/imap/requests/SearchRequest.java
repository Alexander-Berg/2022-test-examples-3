package ru.yandex.autotests.innerpochta.imap.requests;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.autotests.innerpochta.imap.consts.Headers;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.SearchResponse;

import static java.util.Collections.addAll;

public class SearchRequest implements ImapRequestBuilder<SearchResponse> {
    private final List<String> criteria = new ArrayList<>();
    private boolean isUid = false;
    private String charset = "";
    private String sequence = "";


    private SearchRequest() {
    }

    private SearchRequest(String sequence) {
        this.sequence = sequence;
    }

    public static SearchRequest search() {
        return new SearchRequest();
    }

    public static SearchRequest search(String sequence) {
        return new SearchRequest(sequence);
    }

    @Override
    public ImapRequest<SearchResponse> build(String tag) {
        ImapRequest<SearchResponse> request = new ImapRequest(SearchResponse.class, tag);
        if (isUid) {
            request.add(ImapCmd.UID);
        }
        request.add(ImapCmd.SEARCH);
        if (!charset.isEmpty()) {
            request.add("CHARSET", charset);
        }
        if (!sequence.isEmpty()) {
            request.add(sequence);
        }
        for (String criterion : criteria) {
            request.add(criterion);
        }
        return request;
    }

    public SearchRequest uid(boolean value) {
        isUid = value;
        return this;
    }

    public SearchRequest charset(String charset) {
        this.charset = charset;
        return this;
    }

    public SearchRequest criterion(String... parameters) {
        addAll(criteria, parameters);
        return this;
    }

    public SearchRequest all() {
        return criterion("ALL");
    }

    public SearchRequest answered() {
        return criterion("ANSWERED");
    }

    public SearchRequest bcc(String value) {
        return criterion("BCC", value);
    }

    public SearchRequest before(String date) {
        return criterion("BEFORE", date);
    }

    public SearchRequest body(String value) {
        return criterion("BODY", value);
    }

    public SearchRequest cc(String value) {
        return criterion("CC", value);
    }

    public SearchRequest deleted() {
        return criterion("DELETED");
    }

    public SearchRequest draft() {
        return criterion("DRAFT");
    }

    public SearchRequest flagged() {
        return criterion("FLAGGED");
    }

    public SearchRequest from(String value) {
        return criterion("FROM", value);
    }

    public SearchRequest header(String field, String value) {
        return criterion("HEADER", field, value);
    }

    public SearchRequest header(Headers header, String value) {
        return criterion("HEADER", header.toString(), value);
    }

    public SearchRequest keyword(String value) {
        return criterion("KEYWORD", value);
    }

    public SearchRequest larger(String value) {
        return criterion("LARGER", value);
    }

    public SearchRequest newMessages() {
        return criterion("NEW");
    }

    public SearchRequest not(String... argument) {
        addAll(criteria, "NOT");
        addAll(criteria, argument);
        return this;
    }

    public SearchRequest old() {
        return criterion("OLD");
    }

    public SearchRequest on(String date) {
        return criterion("ON", date);
    }

    public SearchRequest or(String[] argument1, String[] argument2) {
        addAll(criteria, "OR");
        addAll(criteria, argument1);
        addAll(criteria, argument2);
        return this;
    }

    public SearchRequest or() {
        criterion("OR");
        return this;
    }

    public SearchRequest recent() {
        return criterion("RECENT");
    }

    public SearchRequest seen() {
        return criterion("SEEN");
    }

    public SearchRequest sentBefore(String date) {
        return criterion("SENTBEFORE", date);
    }

    public SearchRequest sentOn(String date) {
        return criterion("SENTON", date);
    }

    public SearchRequest sentSince(String date) {
        return criterion("SENTSINCE", date);
    }

    public SearchRequest since(String date) {
        return criterion("SINCE", date);
    }

    public SearchRequest smaller(String value) {
        return criterion("SMALLER", value);
    }

    public SearchRequest subject(String value) {
        return criterion("SUBJECT", value);
    }

    public SearchRequest text(String value) {
        return criterion("TEXT", value);
    }

    public SearchRequest to(String value) {
        return criterion("TO", value);
    }

    public SearchRequest uid(String value) {
        return criterion("UID", value);
    }

    public SearchRequest unanswered() {
        return criterion("UNANSWERED");
    }

    public SearchRequest undeleted() {
        return criterion("UNDELETED");
    }

    public SearchRequest undraft() {
        return criterion("UNDRAFT");
    }

    public SearchRequest unflagged() {
        return criterion("UNFLAGGED");
    }

    public SearchRequest unkeyword(String value) {
        return criterion("UNKEYWORD", value);
    }

    public SearchRequest unseen() {
        return criterion("UNSEEN");
    }
}
