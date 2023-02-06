package ru.yandex.autotests.innerpochta.util;

import java.util.LinkedList;

public class TestResultSaver {
    private static TestResultSaver instance;
    private LinkedList<String> results = new LinkedList<>();

    private TestResultSaver(){
    }

    public static TestResultSaver getInstance(){
        if (instance == null){
            instance = new TestResultSaver();
        }
        return instance;
    }

    public void addResult(String result){
        results.add(result);
    }

    public LinkedList<String> getResults(){
        return results;
    }

    public void clearResults(){
        results = new LinkedList<>();
    }

    public boolean isPassedOrFailedInResult() {
        for (String result: results){
            if (result.contains("\"is_passed\":1") || result.contains("\"is_failed\":1")){
                return true;
            }
        }
        return false;
    }
}
