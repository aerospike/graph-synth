/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.test.generator;

import java.util.Arrays;
import java.util.function.Function;

/*
  Created by Grant Haywood grant@iowntheinter.net
  7/21/23
*/
public class SchemaTestConstants {
    public static class SchemaKeys {
        public static final String GOLDEN_ENTITY = "GoldenEntity";
        public static final String COOKIE = "Cookie";
        public static final String CONTACT_MEDIUM = "ContactMedium";
        public static final String INDIVIDUAL = "Individual";
        public static final String HOUSEHOLD = "Household";
        public static final String IP_ADDRESS = "IPAddress";
        public static final String DIGITAL_ENTITY = "DigitalEntity";


        public static final String RESOLVES_TO_INDIVIDUAL = "ResolvesToIndividual";
        public static final String ASSOCIATED_WITH_COOKIE = "AssociatedWithCookie";
        public static final String OBSERVED_DIGITAL_ENTITY = "ObservedDigitalEntity";
        public static final String CONNECTED_FROM_IP = "ConnectedFromIp";
        public static final String HAS_CONTACT_MEDIUM = "HasContactMedium";
        public static final String ASSIGNED_SSN = "AssignedSSN";
        public static final String LIVES_AT = "LivesAt";
    }

    public static void verifyAllCounts(final Function<String, Long> getCount, final long testSize) {
        Arrays.stream(SchemaKeys.class.getDeclaredFields()).map(field -> {
            try {
                return (String) field.get(null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).forEach(schemaKey -> {
            verifyCount(schemaKey, getCount.apply(schemaKey), testSize);
        });
    }

    public static void verifyCount(final String schemaKey, final long count, final long testSize) {
        if (schemaKey.equals(SchemaKeys.GOLDEN_ENTITY))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.COOKIE))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.CONTACT_MEDIUM))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.INDIVIDUAL))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.HOUSEHOLD))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.IP_ADDRESS))
            assert count == 2 * testSize;
        else if (schemaKey.equals(SchemaKeys.DIGITAL_ENTITY))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.RESOLVES_TO_INDIVIDUAL))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.ASSOCIATED_WITH_COOKIE))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.OBSERVED_DIGITAL_ENTITY))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.CONNECTED_FROM_IP))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.HAS_CONTACT_MEDIUM))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.ASSIGNED_SSN))
            assert count == testSize;
        else if (schemaKey.equals(SchemaKeys.LIVES_AT))
            assert count == testSize;
        else
            throw new RuntimeException("unknown test schema key: " + schemaKey);
    }
}
