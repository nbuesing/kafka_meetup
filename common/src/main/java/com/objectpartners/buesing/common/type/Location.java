package com.objectpartners.buesing.common.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Deprecated
@Getter
@RequiredArgsConstructor
@ToString
public class Location {
    private final double latitude;
    private final double longitude;
}
