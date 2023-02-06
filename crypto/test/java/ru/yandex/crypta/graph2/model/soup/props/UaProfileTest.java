package ru.yandex.crypta.graph2.model.soup.props;


import org.junit.Test;

import ru.yandex.crypta.graph2.model.soup.props.UaProfile.UserAgentEquality;

import static org.junit.Assert.assertEquals;

public class UaProfileTest {

    @Test
    public void compareUaProfiles() {
        UserAgentEquality theSame = UaProfile.compareUaProfiles(
                new UaProfile("m|tablet|samsung|android|4.2.2"),
                new UaProfile("m|tablet|samsung|android|4.2.2")
        );
        assertEquals(UserAgentEquality.EQUALS, theSame);

        UserAgentEquality differentVersion = UaProfile.compareUaProfiles(
                new UaProfile("m|tablet|samsung|android|4.2.3"),
                new UaProfile("m|tablet|samsung|android|4.2.2")
        );
        // may be version update
        assertEquals(UserAgentEquality.ROUGHLY_EQUALS, differentVersion);

        UserAgentEquality noVersion = UaProfile.compareUaProfiles(
                new UaProfile("m|tablet|samsung|android|"),
                new UaProfile("m|tablet|samsung|android|")
        );
        assertEquals(UserAgentEquality.NOT_EQUAL, noVersion);

        UserAgentEquality partialUa = UaProfile.compareUaProfiles(
                new UaProfile("m|tablet||android|4.2.3"),
                new UaProfile("m|tablet|samsung|android|4.2.2")
        );
        assertEquals(UserAgentEquality.NOT_EQUAL, partialUa);


    }
}
