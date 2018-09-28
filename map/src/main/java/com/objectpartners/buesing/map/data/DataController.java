package com.objectpartners.buesing.map.data;

import com.objectpartners.buesing.map.type.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping(value = {"/data"})
@Slf4j
public class DataController {

    private final DataService dataService;

    public DataController(final DataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping(value = "/blue")
    public Collection<GeoPoint> getBlue() {
        return dataService.getBlue();
    }

    @GetMapping(value = "/red")
    public Collection<GeoPoint> getRed() {
        return dataService.getRed();
    }

    @GetMapping(value = "/lines")
    public List<DataService.Line> getLines() {
        return dataService.getLines();
    }

    @GetMapping(value = "/allLines")
    public List<DataService.Line> getAllLines() {
        return dataService.getAllLines();
    }

    @GetMapping(value = "/airports")
    public List<Location> getAirports() {

        List<Location> list = new ArrayList<>();

        Location location = new Location(43.9004, -74.822);

        location.setCount(11);

        list.add(location);

        return list;
    }

    @GetMapping(value = "/airportsCount")
    public List<Location> getAirportCounts() {
        return dataService.airportCounts();
    }

    @GetMapping(value = "/grid")
    public List<LineString> getGrid() {
        return dataService.getGrid();
    }

}
