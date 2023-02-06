package ru.yandex.market.robot.auto_generation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Dmitry Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 06.10.17
 */
public class RemovePatternProcessorTest {
    @Test
    public void process() throws Exception {
        RemovePatternProcessor processor = new RemovePatternProcessor("test");
        assertEquals("apple ", processor.process("apple test"));
        processor = new RemovePatternProcessor("Автомат.выкл-ль");
        assertEquals(" test", processor.process("Автомат.выкл-ль test"));
        ReplacePatternProcessor replacePatternProcessor = new ReplacePatternProcessor("1-полюсной|1P");
        assertEquals("1P test", replacePatternProcessor.process("1-полюсной test"));
    }

}