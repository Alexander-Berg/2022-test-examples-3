package ru.yandex.market.pers.notify.ems.event;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         09.06.16
 */
public class NotificationEventDataTest {
	@Test
	public void getMultiData() throws Exception {
		NotificationEventData data = new NotificationEventData();
		data.setMulti(true);
		assertTrue(data.getMultiData().isEmpty());

		Map<String, String> data0 = generateData();
		Map<String, String> data1 = generateData();
		data.addToMultiData(data0);
		data.addToMultiData(data1);
		
		assertEquals(2, data.getMultiData().size());

		assertEquals(data0, data.getMultiData().get(0));
		assertEquals(data1, data.getMultiData().get(1));
	}
	
	private Map<String, String> generateData() {
		Map<String, String> result = new HashMap<>();
		Random rnd = new Random();
		int size = rnd.nextInt(100) + 100;
		for (int i = 0; i < size; i++) {
			result.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
		}
		return result;
	}
	
	@Test
	public void testMakeMulti() throws Exception {
		Map<String, String> dataRaw = generateData();
		NotificationEventData data = new NotificationEventData(dataRaw);
		
		data.makeMulti();
		assertEquals(2, data.size());
		assertEquals(1, data.getMultiData().size());
		assertEquals(dataRaw, data.getMultiData().get(0));
	}
}
