package com.objectpartners.buesing.map.slides;

import com.objectpartners.buesing.avro.Location;
import com.objectpartners.buesing.common.util.BucketFactory;
import com.objectpartners.buesing.map.type.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = {"/slides"})
@Slf4j
public class SlidesController {

    private BucketFactory bucketFactory = new BucketFactory(3.0);

    private Location red = new Location(44.7, -95.5);
    private List<Location> blues = Arrays.asList(
            new Location(42.8, -96.3),
            new Location(41.9, -98.9),
            new Location(39.8, -87.37),
            new Location(42.1, -93.9));

    @GetMapping(value = "/grid")
    public List<LineString> getGrid() {
        return bucketFactory.create(
                new Location(24.396308, -124.848974),
                new Location(49.384358, -66.885444)
        ).stream().map(LineString::new).collect(Collectors.toList());
    }

    @GetMapping(value = "/slideA")
    public List<GeoJson> getColored(@RequestParam(value = "slide", defaultValue = "0") final int slide) {

        log.debug("slideNumber={}", slide);

        List<GeoJson> list = new ArrayList<>();

        GeoPoint redPoint = new GeoPoint(red);
        redPoint.setProperty("style", "fill:red;opacity:1.0;stroke-width:1pt");

        if (slide >= 0) {
            list.add(redPoint);
        }

        if (slide >= 1 && slide < 14) {
            list.addAll(Collections.singleton(bucketFactory.create(red))
                    .stream()
                    .map((bucket) -> {
                        final LineString lineString = new LineString(bucket);
                        lineString.setProperty("style", "fill:red;opacity:0.2;stroke-width:0");
                        return lineString;
                    })
                    .collect(Collectors.toList())
            );
        }

        if (slide <= 1) {
            return list;
        }

        int blueIndex = (slide - 2) / 3;
        int subSlide = (slide - 2) % 3;

        for (int i = 0; i <= blueIndex; i++) {

            if (i >= blues.size()) {
                break;
            }

            Location blue = blues.get(i);

            GeoPoint bluePoint = new GeoPoint(blue);
            bluePoint.setProperty("style", "fill:blue;opacity:1.0;stroke-width:1pt");
            list.add(bluePoint);

            if (i == blueIndex && subSlide >= 1) {
                list.addAll(bucketFactory.createSurronding(blue)
                        .stream()
                        .map((bucket) -> {
                            final LineString lineString = new LineString(bucket);
                            lineString.setProperty("style", "fill:blue;opacity:0.2;stroke-width:0");
                            return lineString;
                        })
                        .collect(Collectors.toList())
                );
            }

            if ((i < blueIndex || subSlide >= 2) && slide < 14) {
                MultiLineString lineString = new MultiLineString();
                if (i == 2) {
                    lineString.setProperty("style", "fill:none;stroke:green;stroke-dasharray:5,5;stroke-width:1");
                } else {
                    lineString.setProperty("style", "fill:none;stroke:green;stroke-width:2");
                }
                lineString.add(red, blue);
                list.add(lineString);
            }

        }

        if (slide >= 14) {
            MultiLineString lineString = new MultiLineString();
            lineString.setProperty("style", "fill:none;stroke:green;stroke-width:2");
            lineString.add(red, blues.get(0));
            list.add(lineString);
        }

        return list;
    }
}
