package ru.yandex.market.pers.notify.push;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.notify.mock.MockFactory;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         17.05.16
 */
public class MobileAppInfoDAOTest extends MockedDbTest {
	private static final ThreadLocalRandom RND = ThreadLocalRandom.current();

	@Autowired
	private MobileAppInfoDAO mobileAppInfoDAO;


	@Test
	public void testAdd() {
		assertTrue(mobileAppInfoDAO.add(MockFactory.generateMobileAppInfo()));
	}
	
	@Test 
	public void testGet() {
		MobileAppInfo expected = MockFactory.generateMobileAppInfo();
		expected.setUid((long)RND.nextInt(10_000));
        expected.setYandexUid(UUID.randomUUID().toString());
        assertTrue(mobileAppInfoDAO.add(expected));

		MobileAppInfo actual = mobileAppInfoDAO.getByUuid(expected.getUuid());
		assertEquals(expected, actual);

		List<MobileAppInfo> infos = mobileAppInfoDAO.getByUid(expected.getUid());
		assertNotNull(infos);
		assertEquals(1, infos.size());
		actual = infos.get(0);
		assertEquals(expected, actual);
        assertEquals(expected.getYandexUid(), actual.getYandexUid());
    }
	
	@Test
	public void testUnregister() {
		MobileAppInfo expected = MockFactory.generateMobileAppInfo();
		expected.setUnregistered(false);
		assertTrue(mobileAppInfoDAO.add(expected));
		
		MobileAppInfo actual = mobileAppInfoDAO.getByUuid(expected.getUuid());
		assertFalse(actual.isUnregistered());

		mobileAppInfoDAO.unregister(expected.getUuid());
		
		actual = mobileAppInfoDAO.getByUuid(expected.getUuid());
		assertTrue(actual.isUnregistered());
	}
	
	@Test
	public void testGetWithLoggedOut() {
		MobileAppInfo expectedLoggedIn = MockFactory.generateMobileAppInfo();
		expectedLoggedIn.setUnregistered(false);
		assertTrue(mobileAppInfoDAO.add(expectedLoggedIn));

		MobileAppInfo expectedLoggedOut = MockFactory.generateMobileAppInfo();
		assertTrue(mobileAppInfoDAO.add(expectedLoggedOut));
		mobileAppInfoDAO.unregister(expectedLoggedOut.getUuid());

		List<MobileAppInfo> infos = mobileAppInfoDAO.getWithLoggedOut();
		assertNotNull(infos);
		assertEquals(2, infos.size());

		assertEquals(1, infos.stream().filter(MobileAppInfo::isUnregistered).count());

		for (MobileAppInfo info : infos) {
			if (info.isUnregistered()) {
				assertEquals(expectedLoggedOut, info);
			} else {
				assertEquals(expectedLoggedIn, info);
			}
		}
	}
	
	@Test
	public void testGetLongOffline() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());

		MobileAppInfo online = MockFactory.generateMobileAppInfo();
		online.setLoginTime(calendar.getTime());
		assertTrue(mobileAppInfoDAO.add(online));
		
		MobileAppInfo longOffline = MockFactory.generateMobileAppInfo();
		calendar.add(Calendar.MINUTE, -2);
		longOffline.setLoginTime(calendar.getTime());
		assertTrue(mobileAppInfoDAO.add(longOffline));

		List<UserInformation> infos = mobileAppInfoDAO.getLongOffline(60, 180);
		assertNotNull(infos);
		assertEquals(1, infos.size());
		UserInformation userInformation = new UserInformation(longOffline.getUid(), longOffline.getUuid(), longOffline.getYandexUid());
		boolean equalInformation = Objects.equals(userInformation.getUuid(), infos.get(0).getUuid()) || Objects.equals(userInformation.getUid(), infos.get(0).getUid());
        assertTrue(equalInformation);
	}

}
