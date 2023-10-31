/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator;


import com.aerospike.graph.synth.emitter.generator.schema.definition.GeneratorConfig;
import com.aerospike.movement.logging.core.Logger;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.github.javafaker.Faker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public abstract class ValueGenerator<T> {
    public static ValueGenerator getGenerator(GeneratorConfig valueGenerator) {
        if (valueGenerator.impl.equals(RandomBoolean.class.getSimpleName())) {
            return RandomBoolean.INSTANCE;
        } else if (valueGenerator.impl.equals(RandomString.class.getSimpleName())) {
            return RandomString.INSTANCE;
        } else if (valueGenerator.impl.equals(JFaker.class.getSimpleName())) {
            return JFaker.INSTANCE;
        } else if (valueGenerator.impl.equals(RandomDigitSequence.class.getSimpleName())) {
            return RandomDigitSequence.INSTANCE;
        } else if (valueGenerator.impl.equals(FormattedRandomSSN.class.getSimpleName())) {
            return FormattedRandomSSN.INSTANCE;
        } else if (valueGenerator.impl.equals(FormattedRandomUSAddress.class.getSimpleName())) {
            return FormattedRandomUSAddress.INSTANCE;
        } else if (valueGenerator.impl.equals(FormattedRandomUSPhone.class.getSimpleName())) {
            return FormattedRandomUSPhone.INSTANCE;
        } else if (valueGenerator.impl.equals(FormattedRandomUSZip.class.getSimpleName())) {
            return FormattedRandomUSZip.INSTANCE;
        } else if (valueGenerator.impl.equals(FormattedDateTime.class.getSimpleName())) {
            return FormattedDateTime.INSTANCE;
        } else if (valueGenerator.impl.equals(RandomUUID.class.getSimpleName())) {
            return RandomUUID.INSTANCE;
        } else if (valueGenerator.impl.equals(FormattedIPV4Address.class.getSimpleName())) {
            return FormattedIPV4Address.INSTANCE;
        } else if (valueGenerator.impl.equals(ChoiceFromList.class.getSimpleName())) {
            return ChoiceFromList.INSTANCE;
        } else {
            try {
                return (ValueGenerator) Class.forName(valueGenerator.impl).getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    abstract public T generate(final Map<String, Object> params);

    public static class RandomBoolean extends ValueGenerator<Boolean> {
        public static RandomBoolean INSTANCE = new RandomBoolean();

        @Override
        public Boolean generate(final Map<String, Object> params) {
            return new Random().nextBoolean();
        }
    }

    public static Object getOrThrow(final Class<? extends ValueGenerator<?>> generator,
                                    final String configKey,
                                    final Map<String, Object> generatorConfig) {
        return Optional.ofNullable(generatorConfig.get(configKey))
                .orElseThrow(() ->
                        RuntimeUtil.getErrorHandler(generator).handleFatalError(
                                new RuntimeException(String.format(
                                        "missing configuration key %s for ValueGenerator %s",
                                        configKey,
                                        generator.getSimpleName()))));
    }

    public static class RandomString extends ValueGenerator<String> {
        public static RandomString INSTANCE = new RandomString();

        private String randomString(int len) {
            final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            final Random rnd = new Random();
            final StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++)
                sb.append(AB.charAt(rnd.nextInt(AB.length())));
            return sb.toString();
        }

        public String generate(final Map<String, Object> params) {
            return randomString((Integer) getOrThrow(this.getClass(), "length", params));
        }
    }

    public static class RandomUUID extends ValueGenerator<String> {
        public static RandomString INSTANCE = new RandomString();

        public String generate(final Map<String, Object> params) {
            return UUID.randomUUID().toString();
        }
    }

    public static class FormattedIPV4Address extends ValueGenerator<String> {
        public static FormattedIPV4Address INSTANCE = new FormattedIPV4Address();

        @Override
        public String generate(final Map<String, Object> params) {
            Long octet1 = RandomDigitSequence.INSTANCE.generate(Map.of("digits", 3));
            Long octet2 = RandomDigitSequence.INSTANCE.generate(Map.of("digits", 3));
            Long octet3 = RandomDigitSequence.INSTANCE.generate(Map.of("digits", 3));
            Long octet4 = RandomDigitSequence.INSTANCE.generate(Map.of("digits", 3));
            return String.format("%s.%s.%s.%s", octet1, octet2, octet3, octet4);
        }
    }

    public static class RandomDigitSequence extends ValueGenerator<Long> {
        public static RandomDigitSequence INSTANCE = new RandomDigitSequence();

        @Override
        public Long generate(final Map<String, Object> params) {
            int digits = (Integer) getOrThrow(this.getClass(), "digits", params);
            StringBuilder sb = new StringBuilder(digits);
            for (int i = 0; i < digits; i++) {
                sb.append(new Random().nextInt(10));
            }
            return Long.valueOf(sb.toString());
        }
    }

    public static class JFaker extends ValueGenerator<String> {
        public static class Keys {
            public static String MODULE = "module";
            public static String METHOD = "method";
        }

        public static JFaker INSTANCE = new JFaker();
        private final Faker faker = new Faker();
        private final List<String> fakerModules = Arrays
                .stream(faker.getClass().getMethods())
                .map(Method::getName)
                .collect(Collectors.toList());
        private final Logger logger = RuntimeUtil.getLogger(JFaker.INSTANCE);

        @Override
        public String generate(final Map<String, Object> params) {
            final Method fakerModuleGetter;
            final Object fakerModule;
            try {
                fakerModuleGetter = RuntimeUtil.getMethod(faker.getClass(), (String) getOrThrow(this.getClass(), Keys.MODULE, params));
                fakerModule = fakerModuleGetter.invoke(faker);
            } catch (Exception e) {
                final String errorMessage = "Faker modules available: " + fakerModules.stream().reduce((a, b) -> a + "\n" + b).orElse("");
                logger.error(errorMessage);
                throw RuntimeUtil.getErrorHandler(JFaker.INSTANCE)
                        .handleFatalError(e, "Cannot find faker module " + getOrThrow(this.getClass(), Keys.MODULE, params), params);
            }
            final Method fakerMethodGetter;
            final Object result;
            try {
                fakerMethodGetter = RuntimeUtil.getMethod(fakerModule.getClass(), (String) getOrThrow(this.getClass(), Keys.METHOD, params));
                result = fakerMethodGetter.invoke(fakerModule);
            } catch (Exception e) {
                final String errorMessage =
                        "Faker methods available on module: " +
                                fakerModule.getClass().getSimpleName() + " are: \n" +
                                Arrays.stream(fakerModule.getClass().getMethods())
                                        .map(Method::getName)
                                        .reduce((a, b) -> a + "\n" + b)
                                        .orElse("");
                logger.error(errorMessage);
                throw RuntimeUtil.getErrorHandler(JFaker.INSTANCE)
                        .handleFatalError(e, "Cannot find or invoke faker method " + getOrThrow(this.getClass(), Keys.METHOD, params), params);
            }

            return result.toString();
        }
    }

    public static class FormattedRandomSSN extends ValueGenerator<String> {
        public static FormattedRandomSSN INSTANCE = new FormattedRandomSSN();

        public String generate(final Map<String, Object> params) {
            return String.format("%03d-%02d-%04d",
                    new Random().nextInt(1000),
                    new Random().nextInt(100),
                    new Random().nextInt(10000));
        }
    }

    public static class FormattedRandomUSPhone extends ValueGenerator<String> {
        public static FormattedRandomUSPhone INSTANCE = new FormattedRandomUSPhone();

        public String generate(final Map<String, Object> params) {
            return String.format("+1 (%03d) %03d-%04d",
                    new Random().nextInt(1000),
                    new Random().nextInt(1000),
                    new Random().nextInt(10000));
        }
    }

    public static class FormattedRandomUSZip extends ValueGenerator<String> {
        public static FormattedRandomUSZip INSTANCE = new FormattedRandomUSZip();

        public String generate(final Map<String, Object> params) {
            return String.format("%05d",
                    new Random().nextInt(100000));
        }
    }

    public static class FormattedDateTime extends ValueGenerator<String> {
        public static FormattedDateTime INSTANCE = new FormattedDateTime();

        public String generate(final Map<String, Object> params) {
            return Date.from(new Date().toInstant().plusSeconds(new Random().nextInt(1000000))).toString();
        }
    }

    public static class FormattedRandomUSAddress extends ValueGenerator<String> {
        private Set<String> streetNames = Set.of(
                "Maple", "Oak", "Pine", "Elm", "Cedar", "Willow", "Birch", "Juniper", "Ash", "Cypress", "Magnolia", "Spruce", "Hawthorn", "Aspen", "Sycamore", "Linden", "Poplar", "Chestnut", "Mulberry", "Redwood", "Dogwood", "Alder", "Cherry", "Walnut", "Beech", "Hazel", "Locust", "Yew", "Hemlock", "Sequoia", "Ginkgo", "Olive", "Acacia", "Baobab", "Eucalyptus", "Mimosa", "Sassafras", "Cactus", "Fig", "Honeysuckle", "Jacaranda", "Kiwi", "Myrtle", "Nectarine", "Pecan", "Plum", "Quince", "Raspberry", "Sage", "Tamarind", "Verbena", "Wisteria", "Xylosma", "Yam", "Zinnia", "Begonia", "Calendula", "Dahlia", "Echinacea");
        private Set<String> streetSuffix = Set.of("ln", "ct", "st", "cir", "sq", "dr", "ave", "rd", "blvd", "way", "pl", "ter", "trl", "pkwy", "hwy");
        public static FormattedRandomUSAddress INSTANCE = new FormattedRandomUSAddress();

        public String generate(final Map<String, Object> params) {
            return String.format("%d %s %s",
                    new Random().nextInt(10000),
                    streetNames.stream().skip(new Random().nextInt(streetNames.size())).findFirst().get(),
                    streetSuffix.stream().skip(new Random().nextInt(streetSuffix.size())).findFirst().get());
        }
    }

    public static class ChoiceFromList extends ValueGenerator<String> {
        public static ChoiceFromList INSTANCE = new ChoiceFromList();

        public String generate(final Map<String, Object> params) {
            List<String> choices = (List<String>) getOrThrow(this.getClass(), "choices", params);
            return choices.stream().skip(new Random().nextInt(choices.size())).findFirst().get();
        }
    }
}
