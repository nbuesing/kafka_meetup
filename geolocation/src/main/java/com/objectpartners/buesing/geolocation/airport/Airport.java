package com.objectpartners.buesing.geolocation.airport;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = { "code" })
public class Airport {

    private final String code;
    private final String name;

    Airport() {
        code = null;
        name = null;
    }
}
