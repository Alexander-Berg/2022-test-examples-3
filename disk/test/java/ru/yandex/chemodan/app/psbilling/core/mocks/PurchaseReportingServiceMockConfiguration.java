package ru.yandex.chemodan.app.psbilling.core.mocks;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.psbilling.core.billing.users.BillingActionsReportingService;

@Configuration
public class PurchaseReportingServiceMockConfiguration {
    public static ListF<String> resHolder = Cf.arrayList();

    @Bean
    public BillingActionsReportingService billingActionsReportingService() {
        return new BillingActionsReportingService() {
            @Override
            protected String composeReportString(ReportData reportData) {
                String result = super.composeReportString(reportData);
                resHolder.add(result);
                return result;
            }
        };
    }


    public static void assertLogLike(String... pattern) {
        assertLogLike(Cf.list(pattern));
    }

    public static void assertLogLike(ListF<String> patterns) {
        if(resHolder.size() < patterns.size()) {
            throw new AssertionError("purchaseReportingService was never called " + patterns.size() + " times");
        }
        for(int i=0; i < patterns.size(); i++) {
            String pattern = StringUtils.replaceEachRepeatedly(patterns.get(i),
                    new String[]{"%uuid%"    , "%uid%" , "%subscription%"              ,
                            "%date%"},
                    new String[]{"[0-9a-z-]+", "[0-9]+", "SubscriptionResponse\\[.+\\]",
                            "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z"}
            );

            //take results from the tail end of resHolder
            int resHoderIndex = resHolder.size() - i-1;
            if(!resHolder.get(resHoderIndex).matches(pattern)){
                throw new AssertionError("Expected pattern \"" + pattern + "\" not matched by report \""
                        + resHolder.get(resHoderIndex) + "\"");
            }
        }
        resHolder.clear();
    }
}
