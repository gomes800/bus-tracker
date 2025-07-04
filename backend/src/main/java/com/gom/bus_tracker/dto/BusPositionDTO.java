package com.gom.bus_tracker.dto;

import lombok.Data;

@Data
public class BusPositionDTO {
    private String latitude;
    private String longitude;
    private String linha;
}
