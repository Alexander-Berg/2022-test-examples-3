package ru.yandex.market.robot.shared.clusterizer;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 02.08.2016
 */
public class ParamProcessorTest {
    @Test
    public void process() throws Exception {
        assertEquals(
            "aa, aa",
            ParamProcessor.TEXT_FORMATTING.process("aa,aa")
        );
        assertEquals(
            "аб, аб",
            ParamProcessor.TEXT_FORMATTING.process("аб,аб")
        );
        assertEquals(
            "aa, aa",
            ParamProcessor.TEXT_FORMATTING.process("aa, aa")
        );
        assertEquals(
            "ab. ab",
            ParamProcessor.TEXT_FORMATTING.process("ab.ab")
        );
        assertEquals(
            "1.1",
            ParamProcessor.TEXT_FORMATTING.process("1.1")
        );
        assertEquals(
            "a a",
            ParamProcessor.TEXT_FORMATTING.process("a\t\n\ra")
        );
        assertEquals(
            "ab им",
            ParamProcessor.TEXT_FORMATTING.process(" \t\r\nab им\t\r\n ")
        );
        assertEquals(
            "я, ты, он, она",
            ParamProcessor.UPPER_LETTER_TO_COMMA.process("Я Ты Он Она")
        );
        assertEquals(
            "сандал, нектарин, бобы тонка, амбра",
            ParamProcessor.STICK_TO_COMMA.process("сандал|нектарин|бобы тонка|амбра")
        );
    }
}