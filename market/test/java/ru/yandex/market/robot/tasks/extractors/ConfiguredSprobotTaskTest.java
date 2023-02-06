package ru.yandex.market.robot.tasks.extractors;

import junit.framework.TestCase;

public class ConfiguredSprobotTaskTest extends TestCase {
    public void test() {

    }
    /*
    private ConfiguredSprobotTask getTask(String config, Consumer<Map<String, List<String>>> onEntity) {
        return new ConfiguredSprobotTask() {
            @Override
            protected ThothAddressLaunchDescriptor fetchLaunchDescriptor() {
                return new ThothAddressLaunchDescriptor(config);
            }

            @Override
            protected int getEntityId() {
                return 0;
            }

            @Override
            protected void closeStorage() throws TaskBrokenException { }

            @Override
            protected void consumeResultEntity(Map<String, List<String>> entity) {
                onEntity.accept(entity);
            }

            @Override
            protected EntityStorage getStorage(LocalDatabase localDatabase, Entity entity,
                                               boolean dropTable) throws TaskBrokenException {
                return null;
            }
        };
    }

    private List<Map<String, String>> runOn(String config) {
        try {
            List<Map<String, String>> result = Cf.list();

            RobotTask task = getTask(config, e -> {
                Map<String, String> mappedEntity = Cf.newHashMap();

                e.forEach((key, values) -> {
                    assert (values.size() == 1);
                    mappedEntity.put(key, values.get(0));
                });

                result.add(mappedEntity);
            });

            task.init(null);
            task.start(null);

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String feedToString(List<Map<String, String>> entities) {
        StringBuilder result = new StringBuilder("; ");

        entities.stream()
                .forEach(m -> {
                            result.delete(result.length() - 2, result.length());
                            result.append("\n");
                            m.entrySet().stream()
                                    .forEach(e -> result.append(e.getKey() + " -> " + e.getValue() + "; "));
                        }
                );

        String res = result.toString();

        return res.substring(1, res.length() - 2);
    }

    // вообще в принципе работает
    public void testWorking() {
        runOn("log('Hello world!');");
    }

    // может вернуть сущность
    public void testCanReturnEntities() {
        String feed = feedToString(runOn(
                "'http://ya.ru'.submit({sp_address:'Москва, ул. Пушкина, д.2'});" +
                        "'http://ya.ru'.submit({sp_address:'Москва, ул. Пушкина, д.3'});"
        ));
        assert (feed.equals(
                "address -> [\"Москва, ул. Пушкина, д.2\"]\n" +
                        "address -> [\"Москва, ул. Пушкина, д.3\"]"
        ));
    }

    public void testFeatureGlue() {
        String feed = feedToString(runOn(
                "'http://ya.ru'.submit({ " +
                        "'feature-boolean:my_feature_name':'true', " +
                        "'feature-enum-multiple:type':['type_5', 'type_7'] " +
                        "});"
        ));

        assert (feed.equals("feature-structurized -> " +
                "[\"boolean:my_feature_name:true\",\"enum-multiple:type:type_5\",\"enum-multiple:type:type_7\"]"));
    }

    public void testFieldGlue() {
        String feed = feedToString(runOn(
                "'http://ya.ru'.submit({ " +
                        "name: 'test', " +
                        "sp_company_name: 'test_2', " +
                        "'name-tr': 'test_3', " +
                        "'name-alt-tr': 'test_4' " +
                        "});"
        ));

        assert (feed.equals("name -> [\"test\",\"test_2\",\"test_3\",\"test_4\"]"));
    }
    */
}
