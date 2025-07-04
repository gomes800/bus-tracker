package com.gom.bus_tracker.controller;

import com.gom.bus_tracker.dto.BusPositionDTO;
import com.gom.bus_tracker.service.BusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bus")
public class BusController {

    @Autowired
    private BusService busService;

    @GetMapping("/positions")
    public ResponseEntity<List<BusPositionDTO>> getBusPositions() {
        List<BusPositionDTO> positions = busService.getCurrentBusPositions();
        return ResponseEntity.ok(positions);
    }

}
